package com.cart.checkout.api.dto;

import com.cart.checkout.domain.cart.CartItem;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(
        UUID id,
        UUID productId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {

    public static CartItemResponse from(CartItem item) {
        return new CartItemResponse(
                item.getId(),
                item.getProductId(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal()
        );
    }
}
