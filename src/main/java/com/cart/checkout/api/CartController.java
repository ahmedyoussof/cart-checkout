package com.cart.checkout.api;

import com.cart.checkout.api.dto.AddItemRequest;
import com.cart.checkout.api.dto.CartResponse;
import com.cart.checkout.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/carts")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping
    public ResponseEntity<CartResponse> create() {
        var cart = cartService.createCart();
        return ResponseEntity.status(HttpStatus.CREATED).body(CartResponse.from(cart));
    }

    @GetMapping("/{cartId}")
    public ResponseEntity<CartResponse> find(@PathVariable UUID cartId) {
        var cart = cartService.find(cartId);
        return ResponseEntity.ok(CartResponse.from(cart));
    }

    @PostMapping("/{cartId}/items")
    public ResponseEntity<CartResponse> addItem(
            @PathVariable UUID cartId,
            @Valid @RequestBody AddItemRequest request) {
        var cart = cartService.addItem(cartId, request);
        return ResponseEntity.ok(CartResponse.from(cart));
    }
}
