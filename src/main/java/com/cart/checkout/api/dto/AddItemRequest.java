package com.cart.checkout.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record AddItemRequest(
        @NotNull UUID productId,
        @Positive int quantity ,
        @NotNull @DecimalMin("1.00") BigDecimal unitPrice
) {
}
