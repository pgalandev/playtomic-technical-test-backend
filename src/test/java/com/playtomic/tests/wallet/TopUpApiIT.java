package com.playtomic.tests.wallet;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.jayway.jsonpath.JsonPath;
import com.playtomic.tests.wallet.support.JwtTestHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
import org.springframework.test.web.servlet.ResultActions;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.playtomic.tests.wallet.helper.WalletManagerConstants.TRANSACTION_COLLECTION;
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
public class TopUpApiIT {

    private static final String CARD = "4242424242424242";
    private static final WireMockServer STRIPE = new WireMockServer(9999);

    @Autowired
    MockMvc mockMvc;

    @Autowired
    MongoTemplate mongoTemplate;

    @BeforeAll
    static void startStripe() {
        STRIPE.start();
    }

    @AfterAll
    static void stopStripe() {
        STRIPE.stop();
    }

    @BeforeEach
    void reset() {
        STRIPE.resetAll();
        STRIPE.stubFor(WireMock.post(anyUrl()).willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody("{\"id\":\"pay_123\"}")));
        mongoTemplate.remove(new Query(), WALLET_COLLECTION);
        mongoTemplate.remove(new Query(), TRANSACTION_COLLECTION);
    }

    @Test
    void topsUpTheWalletAndExposesOnlyTheLastFourDigits() throws Exception {
        String walletId = createWalletFor("user-1");

        String body = topUp(walletId, "user-1", "key-1", "25.00")
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION,
                        matchesPattern(".*/api/v1/wallets/" + walletId + "/top-ups/[^/]+")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.walletId").value(walletId))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.amount.amount").value("25.00"))
                .andExpect(jsonPath("$.amount.currency").value("EUR"))
                .andExpect(jsonPath("$.cardLast4").value("4242"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain(CARD);

        STRIPE.verify(1, postRequestedFor(anyUrl())
                .withRequestBody(equalToJson("{\"credit_card\":\"" + CARD + "\",\"amount\":25.00}", true, true)));
        assertBalance(walletId, "user-1", "25.00");
    }

    @Test
    void accumulatesSuccessiveTopUps() throws Exception {
        String walletId = createWalletFor("user-1");

        topUp(walletId, "user-1", "key-1", "10.00").andExpect(status().isCreated());
        topUp(walletId, "user-1", "key-2", "15.50").andExpect(status().isCreated());

        assertBalance(walletId, "user-1", "25.50");
    }

    @Test
    void neverPersistsTheFullCardNumber() throws Exception {
        String walletId = createWalletFor("user-1");
        topUp(walletId, "user-1", "key-1", "25.00").andExpect(status().isCreated());

        assertThat(mongoTemplate.getCollection(TRANSACTION_COLLECTION).find().first().toJson())
                .doesNotContain(CARD)
                .contains("\"type\": \"TOP_UP\"", "\"status\": \"SUCCEEDED\"", "pay_123", "4242");
    }

    @Test
    void returnsTheTopUpByItsId() throws Exception {
        String walletId = createWalletFor("user-1");
        String topUpId = JsonPath.read(topUp(walletId, "user-1", "key-1", "25.00")
                .andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/v1/wallets/" + walletId + "/top-ups/" + topUpId)
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearerFor("user-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(topUpId))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.cardLast4").value("4242"));
    }

    @Test
    void hidesTopUpsOfOtherPlayersAndUnknownOnes() throws Exception {
        String walletId = createWalletFor("user-1");
        String topUpId = JsonPath.read(topUp(walletId, "user-1", "key-1", "25.00")
                .andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/v1/wallets/" + walletId + "/top-ups/" + topUpId)
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearerFor("user-2")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(endsWith("wallet-not-found")));

        mockMvc.perform(get("/api/v1/wallets/" + walletId + "/top-ups/unknown")
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearerFor("user-1")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(endsWith("top-up-not-found")));
    }

    @Test
    void doesNotTopUpAnotherPlayersWallet() throws Exception {
        String walletId = createWalletFor("user-1");

        topUp(walletId, "user-2", "key-1", "25.00")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(endsWith("wallet-not-found")));

        STRIPE.verify(0, postRequestedFor(anyUrl()));
        assertBalance(walletId, "user-1", "0.00");
    }

    @Test
    void requiresAnIdempotencyKey() throws Exception {
        String walletId = createWalletFor("user-1");

        mockMvc.perform(post("/api/v1/wallets/" + walletId + "/top-ups")
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearerFor("user-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(topUpBody("25.00")))
                .andExpect(status().isBadRequest());

        STRIPE.verify(0, postRequestedFor(anyUrl()));
        assertThat(mongoTemplate.getCollection(TRANSACTION_COLLECTION).countDocuments()).isZero();
    }

    @Test
    void rejectsTopUpsWithoutAValidToken() throws Exception {
        mockMvc.perform(post("/api/v1/wallets/any/top-ups")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(topUpBody("25.00")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/wallets/any/top-ups/any"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidPayloadsWithValidationError() throws Exception {
        String walletId = createWalletFor("user-1");
        String[] bodies = {
                "{\"creditCardNumber\":\"" + CARD + "\"}",
                body("0", "EUR", CARD), body("-5.00", "EUR", CARD), body("10.123", "EUR", CARD),
                body("abc", "EUR", CARD), body("25.00", "USD", CARD),
                "{\"amount\":{\"amount\":\"25.00\"},\"creditCardNumber\":\"" + CARD + "\"}",
                "{\"amount\":{\"amount\":\"25.00\",\"currency\":\"EUR\"}}"
        };
        for (String body : bodies) {
            mockMvc.perform(post("/api/v1/wallets/" + walletId + "/top-ups")
                            .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearerFor("user-1"))
                            .header("Idempotency-Key", "k")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value(endsWith("validation-error")));
        }
        STRIPE.verify(0, postRequestedFor(anyUrl()));
        assertThat(mongoTemplate.getCollection(TRANSACTION_COLLECTION).countDocuments()).isZero();
    }

    @Test
    void rejectsAmountsBelowTheMinimumWithoutCallingStripe() throws Exception {
        String walletId = createWalletFor("user-1");

        topUp(walletId, "user-1", "key-1", "9.99")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value(endsWith("amount-too-small")));

        STRIPE.verify(0, postRequestedFor(anyUrl()));
        assertThat(mongoTemplate.getCollection(TRANSACTION_COLLECTION).countDocuments()).isZero();
    }

    @Test
    void failsTheTopUpWhenStripeRejectsTheAmount() throws Exception {
        String walletId = createWalletFor("user-1");
        stripeRespondsWith(422);

        topUp(walletId, "user-1", "key-1", "25.00")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value(endsWith("amount-too-small")));

        assertSingleFailedTopUp(walletId, "AMOUNT_TOO_SMALL");
    }

    @Test
    void failsTheTopUpOnOtherStripeErrors() throws Exception {
        String walletId = createWalletFor("user-1");
        stripeRespondsWith(500);

        topUp(walletId, "user-1", "key-1", "25.00")
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.type").value(endsWith("payment-provider-error")));

        assertSingleFailedTopUp(walletId, "PAYMENT_PROVIDER_ERROR");

        stripeRespondsWith(400);
        topUp(walletId, "user-1", "key-2", "25.00").andExpect(status().isBadGateway());
        assertBalance(walletId, "user-1", "0.00");
    }

    private void assertSingleFailedTopUp(String walletId, String reason) throws Exception {
        String topUpId = mongoTemplate.getCollection(TRANSACTION_COLLECTION).find().first().get("_id").toString();
        mockMvc.perform(get("/api/v1/wallets/" + walletId + "/top-ups/" + topUpId)
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearerFor("user-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value(reason));
        assertBalance(walletId, "user-1", "0.00");
    }

    private void stripeRespondsWith(int status) {
        STRIPE.resetAll();
        STRIPE.stubFor(WireMock.post(anyUrl()).willReturn(aResponse().withStatus(status)));
    }

    private static String body(String amount, String currency, String card) {
        return "{\"amount\":{\"amount\":\"" + amount + "\",\"currency\":\"" + currency
                + "\"},\"creditCardNumber\":\"" + card + "\"}";
    }

    private ResultActions topUp(String walletId, String user, String key, String amount) throws Exception {
        return mockMvc.perform(post("/api/v1/wallets/" + walletId + "/top-ups")
                .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearerFor(user))
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(topUpBody(amount)));
    }

    private static String topUpBody(String amount) {
        return "{\"amount\":{\"amount\":\"" + amount + "\",\"currency\":\"EUR\"},\"creditCardNumber\":\"" + CARD + "\"}";
    }

    private String createWalletFor(String user) throws Exception {
        String body = mockMvc.perform(post("/api/v1/wallets")
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearerFor(user)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private void assertBalance(String walletId, String user, String expected) throws Exception {
        mockMvc.perform(get("/api/v1/wallets/" + walletId)
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearerFor(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance.amount").value(expected));
    }
}
