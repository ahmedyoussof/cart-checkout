package com.cart.checkout.domain.cart;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

class CartTest {

    @Test
    void create_shouldInitializeWithOpenStatus() {
        Cart cart = Cart.create();

        assertEquals(CartStatus.OPEN, cart.getStatus());
    }

    @Test
    void create_shouldInitializeWithZeroTotal() {
        Cart cart = Cart.create();

        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY), cart.getTotal());
    }

    @Test
    void create_shouldInitializeWithEmptyItems() {
        Cart cart = Cart.create();

        assertNotNull(cart.getItems());
        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void create_shouldGenerateNonNullId() {
        Cart cart = Cart.create();

        assertNotNull(cart.getId());
    }

    @Test
    void lock_shouldTransitionFromOpenToLocked() {
        Cart cart = Cart.create();
        assertEquals(CartStatus.OPEN, cart.getStatus(), "precondition: cart starts OPEN");

        cart.lock();

        assertEquals(CartStatus.LOCKED, cart.getStatus());
    }

    @Test
    void lock_whenAlreadyLocked_shouldBeIdempotent() {
        Cart cart = Cart.create();
        cart.lock();
        assertEquals(CartStatus.LOCKED, cart.getStatus(), "precondition: cart is LOCKED");

        cart.lock();

        assertEquals(CartStatus.LOCKED, cart.getStatus());
    }

    @Test
    void getItems_shouldReturnUnmodifiableList() {
        Cart cart = Cart.create();

        assertThrows(UnsupportedOperationException.class, () -> cart.getItems().add(null));
    }
}
