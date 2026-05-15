package com.cart.checkout.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record WebhookPayload(
        @NotNull UUID paymentId,
        @NotBlank String providerPaymentId,
        @NotNull Outcome outcome
) {

    public enum Outcome {
        CONFIRMED,
        FAILED
    }
}
