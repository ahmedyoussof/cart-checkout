package com.cart.checkout.integration;

import com.cart.checkout.api.dto.AddItemRequest;
import com.cart.checkout.api.dto.CartResponse;
import com.cart.checkout.api.dto.OrderResponse;
import com.cart.checkout.api.dto.PaymentStartResponse;
import com.cart.checkout.api.dto.WebhookPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DuplicateWebhookIT {

    @LocalServerPort
    int port;

    private RestClient client;

    @BeforeEach
    void setup() {
        client = RestClient.builder().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void duplicateWebhook_isIdempotent() {
        UUID cartId = client.post().uri("/carts").retrieve().toEntity(CartResponse.class).getBody().id();
        client.post().uri("/carts/{id}/items", cartId)
                .body(new AddItemRequest(UUID.randomUUID(), 1, new BigDecimal("10.00")))
                .retrieve().toEntity(CartResponse.class);
        OrderResponse order = client.post().uri("/carts/{id}/checkout", cartId)
                .retrieve().toEntity(OrderResponse.class).getBody();
        PaymentStartResponse payment = client.post().uri("/orders/{id}/payment/start", order.id())
                .retrieve().toEntity(PaymentStartResponse.class).getBody();

        WebhookPayload payload = new WebhookPayload(
                payment.paymentId(), payment.providerPaymentId(), WebhookPayload.Outcome.CONFIRMED);

        var first = client.post().uri("/payments/webhook").body(payload).retrieve().toBodilessEntity();
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);

        OrderResponse afterFirst = client.get().uri("/orders/{id}", order.id())
                .retrieve().toEntity(OrderResponse.class).getBody();
        assertThat(afterFirst.status()).isEqualTo("PAID");

        var second = client.post().uri("/payments/webhook").body(payload).retrieve().toBodilessEntity();
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);

        OrderResponse afterSecond = client.get().uri("/orders/{id}", order.id())
                .retrieve().toEntity(OrderResponse.class).getBody();
        assertThat(afterSecond.status()).isEqualTo("PAID");

        WebhookPayload conflictingReplay = new WebhookPayload(
                payment.paymentId(), payment.providerPaymentId(), WebhookPayload.Outcome.FAILED);
        var third = client.post().uri("/payments/webhook").body(conflictingReplay).retrieve().toBodilessEntity();
        assertThat(third.getStatusCode()).isEqualTo(HttpStatus.OK);

        OrderResponse afterThird = client.get().uri("/orders/{id}", order.id())
                .retrieve().toEntity(OrderResponse.class).getBody();
        assertThat(afterThird.status()).isEqualTo("PAID");
    }
}
