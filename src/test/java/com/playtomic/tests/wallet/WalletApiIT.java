package com.playtomic.tests.wallet;

import com.playtomic.tests.wallet.support.JwtTestHelper;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.playtomic.tests.wallet.helper.WalletManagerConstants.WALLET_COLLECTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class WalletApiIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanDatabase() {
        mongoTemplate.remove(new Query(), WALLET_COLLECTION);
    }

    @Test
    void createsAnEmptyEurWalletForTheAuthenticatedUser() throws Exception {
        mockMvc.perform(post("/api/v1/wallets")
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearerFor("user-1")))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, matchesPattern(".*/api/v1/wallets/[^/]+")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.balance.amount").value("0.00"))
                .andExpect(jsonPath("$.balance.currency").value("EUR"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    private String createWalletFor(String user) throws Exception {
        String body = mockMvc.perform(post("/api/v1/wallets")
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearerFor(user)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(body, "$.id");
    }

    @Test
    void returnsTheCallersOwnWallet() throws Exception {
        String id = createWalletFor("user-1");

        mockMvc.perform(get("/api/v1/wallets/" + id)
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearerFor("user-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.balance.amount").value("0.00"))
                .andExpect(jsonPath("$.balance.currency").value("EUR"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.transactions").doesNotExist());
    }

    @Test
    void returnsTheSameNotFoundForUnknownAndForeignWallets() throws Exception {
        String foreignId = createWalletFor("user-2");

        String unknown = mockMvc.perform(get("/api/v1/wallets/does-not-exist")
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearerFor("user-1")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(endsWith("wallet-not-found")))
                .andReturn().getResponse().getContentAsString();

        String foreign = mockMvc.perform(get("/api/v1/wallets/" + foreignId)
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearerFor("user-1")))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        assertThat(foreign.replace(foreignId, "X")).isEqualTo(unknown.replace("does-not-exist", "X"));
    }

    @Test
    void rejectsGetWithoutAValidToken() throws Exception {
        mockMvc.perform(get("/api/v1/wallets/any"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsRequestsWithoutAValidToken() throws Exception {
        mockMvc.perform(post("/api/v1/wallets"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(endsWith("unauthorized")));

        String forgedToken = JwtTestHelper.tokenFor("user-1", "another-secret-another-secret-another-secret");
        mockMvc.perform(post("/api/v1/wallets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + forgedToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsTokensWithoutASubject() throws Exception {
        mockMvc.perform(post("/api/v1/wallets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + JwtTestHelper.tokenWithoutSubject()))
                .andExpect(status().isUnauthorized());
        assertThat(mongoTemplate.getCollection(WALLET_COLLECTION).countDocuments()).isZero();
    }

    @Test
    void rejectsASecondWalletForTheSameUser() throws Exception {
        mockMvc.perform(post("/api/v1/wallets")
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearerFor("user-1")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/wallets")
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearerFor("user-1")))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(endsWith("wallet-already-exists")));
    }

    @Test
    void createsExactlyOneWalletWhenRequestsRaceForTheSameUser() throws Exception {
        int attempts = 8;
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            results.add(pool.submit(() -> {
                start.await();
                return mockMvc.perform(post("/api/v1/wallets")
                                .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearerFor("racer")))
                        .andReturn().getResponse().getStatus();
            }));
        }
        start.countDown();

        List<Integer> statuses = new ArrayList<>();
        try {
            for (Future<Integer> result : results) {
                statuses.add(result.get(30, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(statuses).containsOnlyOnce(201);
        assertThat(statuses).filteredOn(s -> s != 201).containsOnly(409);
    }

    @Test
    void ignoresAnyUserIdSentInTheBody() throws Exception {
        mockMvc.perform(post("/api/v1/wallets")
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearerFor("user-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"user-2\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/wallets")
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearerFor("user-2")))
                .andExpect(status().isCreated());
    }

    @Test
    void storesTheBalanceAsDecimal128() throws Exception {
        mockMvc.perform(post("/api/v1/wallets")
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearerFor("user-1")))
                .andExpect(status().isCreated());

        Document stored = mongoTemplate.getCollection(WALLET_COLLECTION).find().first();
        assertThat(stored.get("balance")).isInstanceOf(Decimal128.class);
    }
}
