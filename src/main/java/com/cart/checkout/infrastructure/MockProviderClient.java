package com.cart.checkout.infrastructure;

import com.cart.checkout.api.dto.WebhookPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MockProviderClient {

    private static final Logger log = LoggerFactory.getLogger(MockProviderClient.class);

    private final RestClient restClient;

    public MockProviderClient(@Value("${mock.provider.callback-base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public void sendWebhook(WebhookPayload payload) {
        log.info("Mock provider dispatching webhook paymentId={} providerPaymentId={} outcome={}",
                payload.paymentId(), payload.providerPaymentId(), payload.outcome());
        restClient.post()
                .uri("/payments/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }
}
