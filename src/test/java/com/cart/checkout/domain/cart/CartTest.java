package com.cart.checkout.domain.cart;

import com.cart.checkout.exceptions.CartLockedException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

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
    void lock_whenAlreadyLocked_shouldThrowCartLockedException() {
        Cart cart = Cart.create();
        cart.lock();
        assertEquals(CartStatus.LOCKED, cart.getStatus(), "precondition: cart is LOCKED");

        CartLockedException ex = assertThrows(CartLockedException.class, cart::lock);
        assertTrue(ex.getMessage().contains(cart.getId().toString()));
    }

    @Test
    void getItems_shouldReturnUnmodifiableList() {
        Cart cart = Cart.create();

        assertThrows(UnsupportedOperationException.class, () -> cart.getItems().add(null));
    }

    @Test
    void addItem_shouldAppendItemAndUpdateTotal() {
        Cart cart = Cart.create();
        UUID productId = UUID.randomUUID();

        cart.addItem(productId, 2, new BigDecimal("50.00"));

        assertEquals(1, cart.getItems().size());
        CartItem item = cart.getItems().get(0);
        assertEquals(productId, item.getProductId());
        assertEquals(2, item.getQuantity());
        assertEquals(new BigDecimal("50.00"), item.getUnitPrice());
        assertEquals(new BigDecimal("100.00"), item.getLineTotal());
        assertEquals(new BigDecimal("100.00"), cart.getTotal());
    }

    @Test
    void addItem_multipleItems_shouldAccumulateTotal() {
        Cart cart = Cart.create();

        cart.addItem(UUID.randomUUID(), 2, new BigDecimal("50.00"));
        cart.addItem(UUID.randomUUID(), 3, new BigDecimal("10.50"));

        assertEquals(2, cart.getItems().size());
        assertEquals(new BigDecimal("131.50"), cart.getTotal());
    }

    @Test
    void addItem_whenLocked_shouldThrowCartLockedException() {
        Cart cart = Cart.create();
        cart.lock();

        CartLockedException ex = assertThrows(CartLockedException.class,
                () -> cart.addItem(UUID.randomUUID(), 1, new BigDecimal("10.00")));
        assertTrue(ex.getMessage().contains(cart.getId().toString()));
    }

    @Test
    void addItem_withZeroQuantity_shouldThrow() {
        Cart cart = Cart.create();

        assertThrows(IllegalArgumentException.class,
                () -> cart.addItem(UUID.randomUUID(), 0, new BigDecimal("10.00")));
    }

    @Test
    void addItem_withNegativeUnitPrice_shouldThrow() {
        Cart cart = Cart.create();

        assertThrows(IllegalArgumentException.class,
                () -> cart.addItem(UUID.randomUUID(), 1, new BigDecimal("-1.00")));
    }

    @Test
    void addItem_withZeroUnitPrice_shouldBeAllowed() {
        Cart cart = Cart.create();

        cart.addItem(UUID.randomUUID(), 5, new BigDecimal("0.00"));

        assertEquals(new BigDecimal("0.00"), cart.getTotal());
    }
}
