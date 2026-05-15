package com.cart.checkout.api.dto;

import com.cart.checkout.domain.cart.Cart;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID id,
        String status,
        List<CartItemResponse> items,
        BigDecimal total
) {

    public static CartResponse from(Cart cart) {
        return new CartResponse(
                cart.getId(),
                cart.getStatus().name(),
                cart.getItems().stream().map(CartItemResponse::from).toList(),
                cart.getTotal()
        );
    }
}
