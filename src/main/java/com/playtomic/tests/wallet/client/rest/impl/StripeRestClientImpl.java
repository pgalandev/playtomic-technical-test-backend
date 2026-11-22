package com.playtomic.tests.wallet.client.rest.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.playtomic.tests.wallet.client.rest.StripeRestClient;
import com.playtomic.tests.wallet.config.client.stripe.StripeClientConfiguration.StripeEndpoints;
import com.playtomic.tests.wallet.domain.external.stripe.Payment;
import com.playtomic.tests.wallet.domain.exception.StripeAmountTooSmallException;
import com.playtomic.tests.wallet.domain.exception.StripeServiceException;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Component
public class StripeRestClientImpl implements StripeRestClient {

    private final StripeEndpoints endpoints;
    private final WebClient webClient;

    public StripeRestClientImpl(WebClient stripeWebClient, StripeEndpoints endpoints) {
        this.webClient = stripeWebClient;
        this.endpoints = endpoints;
    }

    @Override
    public Payment charge(@NonNull String creditCardNumber, @NonNull BigDecimal amount) throws StripeServiceException {
        return webClient.post()
                .uri(endpoints.chargesUri())
                .bodyValue(new ChargeRequest(creditCardNumber, amount))
                .retrieve()
                .onStatus(status -> status == HttpStatus.UNPROCESSABLE_ENTITY,
                        response -> response.releaseBody().then(Mono.error(new StripeAmountTooSmallException())))
                .onStatus(HttpStatusCode::isError,
                        response -> response.releaseBody().then(Mono.error(new StripeServiceException())))
                .bodyToMono(Payment.class)
                .block();
    }

    @Override
    public void refund(@NonNull String paymentId) throws StripeServiceException {
        webClient.post()
                .uri(endpoints.refundsUriTemplate(), paymentId)
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        response -> response.releaseBody().then(Mono.error(new StripeServiceException())))
                .toBodilessEntity()
                .block();
    }

    private record ChargeRequest(@JsonProperty("credit_card") String creditCardNumber,
                                 @JsonProperty("amount") BigDecimal amount) {
    }
}
