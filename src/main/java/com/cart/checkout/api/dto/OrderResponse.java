package com.cart.checkout.api.dto;

import com.cart.checkout.domain.order.Order;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID cartId,
        String status,
        List<OrderItemResponse> items,
        BigDecimal totalAmount
) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCartId(),
                order.getStatus().name(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                order.getTotalAmount()
        );
    }
}
