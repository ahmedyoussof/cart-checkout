package com.cart.checkout.integration;

import com.cart.checkout.api.dto.AddItemRequest;
import com.cart.checkout.api.dto.CartResponse;
import com.cart.checkout.api.dto.OrderResponse;
import com.cart.checkout.api.dto.PaymentStartResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    properties = {
        "server.port=8080",
        "mock.provider.callback-base-url=http://localhost:8080"
    }
)
class CheckoutIT {

    private RestClient client;


    @BeforeEach
    void setup() {
        client = RestClient.builder().baseUrl("http://localhost:8080").build();
    }

    @Test
    void cartCheckout_whenPaymentConfirmed_endsInPaidOrder() {
        var cartResp = client.post().uri("/carts").retrieve().toEntity(CartResponse.class);
        assertThat(cartResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID cartId = cartResp.getBody().id();
        assertThat(cartResp.getBody().status()).isEqualTo("OPEN");

        AddItemRequest item = new AddItemRequest(UUID.randomUUID(), 2, new BigDecimal("12.50"));
        var addResp = client.post().uri("/carts/{id}/items", cartId)
                .body(item).retrieve().toEntity(CartResponse.class);
        assertThat(addResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(addResp.getBody().total()).isEqualByComparingTo("25.00");

        var orderResp = client.post().uri("/carts/{id}/checkout", cartId)
                .retrieve().toEntity(OrderResponse.class);
        assertThat(orderResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        OrderResponse order = orderResp.getBody();
        assertThat(order.status()).isEqualTo("CREATED");
        assertThat(order.cartId()).isEqualTo(cartId);
        assertThat(order.totalAmount()).isEqualByComparingTo("25.00");

        var startResp = client.post().uri("/orders/{id}/payment/start", order.id())
                .retrieve().toEntity(PaymentStartResponse.class);
        assertThat(startResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        PaymentStartResponse payment = startResp.getBody();
        assertThat(payment.status()).isEqualTo("INITIATED");
        assertThat(payment.providerPaymentId()).isNotBlank();

        var pendingResp = client.get().uri("/orders/{id}", order.id())
                .retrieve().toEntity(OrderResponse.class);
        assertThat(pendingResp.getBody().status()).isEqualTo("PENDING_PAYMENT");

        var triggerResp = client.post()
                .uri("/mock-provider/{ppid}/trigger?outcome=CONFIRMED", payment.providerPaymentId())
                .retrieve().toBodilessEntity();
        assertThat(triggerResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        var finalResp = client.get().uri("/orders/{id}", order.id())
                .retrieve().toEntity(OrderResponse.class);
        assertThat(finalResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(finalResp.getBody().status()).isEqualTo("PAID");
    }

}
