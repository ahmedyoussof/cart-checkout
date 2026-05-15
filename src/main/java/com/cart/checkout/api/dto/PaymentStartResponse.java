package com.cart.checkout.api.dto;

import com.cart.checkout.domain.payment.Payment;

import java.util.UUID;

public record PaymentStartResponse(
        UUID paymentId,
        UUID orderId,
        String providerPaymentId,
        String status
) {

    public static PaymentStartResponse from(Payment payment) {
        return new PaymentStartResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getProviderPaymentId(),
                payment.getStatus().name()
        );
    }
}
