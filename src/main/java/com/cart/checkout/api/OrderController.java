package com.cart.checkout.api;

import com.cart.checkout.api.dto.OrderResponse;
import com.cart.checkout.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> find(@PathVariable UUID orderId) {
        var order = orderService.find(orderId);
        return ResponseEntity.ok(OrderResponse.from(order));
    }
}
