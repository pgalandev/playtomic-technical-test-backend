package com.playtomic.tests.wallet.config.client.stripe;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.net.URI;
import java.time.Duration;

@Configuration
public class StripeClientConfiguration {

    @Bean
    WebClient stripeWebClient(WebClient.Builder webClientBuilder,
                              @Value("${stripe.connect-timeout}") Duration connectTimeout,
                              @Value("${stripe.read-timeout}") Duration readTimeout) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeout.toMillis())
                .responseTimeout(readTimeout);
        return webClientBuilder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Bean
    StripeEndpoints stripeEndpoints(@Value("${stripe.simulator.charges-uri}") URI chargesUri,
                                    @Value("${stripe.simulator.refunds-uri}") String refundsUriTemplate) {
        return new StripeEndpoints(chargesUri, refundsUriTemplate);
    }

    public record StripeEndpoints(URI chargesUri, String refundsUriTemplate) {
    }
}
