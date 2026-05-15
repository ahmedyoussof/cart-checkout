package com.cart.checkout.domain.order;

import com.cart.checkout.domain.cart.Cart;
import com.cart.checkout.domain.cart.CartItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    @Test
    void fromCart_shouldSnapshotStatusAsCreated() {
        Cart cart = Cart.create();

        Order order = Order.fromCart(cart);

        assertEquals(OrderStatus.CREATED, order.getStatus());
    }

    @Test
    void fromCart_shouldPreserveCartId() {
        Cart cart = Cart.create();

        Order order = Order.fromCart(cart);

        assertEquals(cart.getId(), order.getCartId());
    }

    @Test
    void fromCart_shouldGenerateNewOrderIdDistinctFromCartId() {
        Cart cart = Cart.create();

        Order order = Order.fromCart(cart);

        assertNotNull(order.getId());
        assertNotEquals(cart.getId(), order.getId());
    }

    @Test
    void fromCart_shouldCopyTotalIntoTotalAmount() {
        Cart cart = Cart.create();
        cart.addItem(UUID.randomUUID(), 2, new BigDecimal("50.00"));
        cart.addItem(UUID.randomUUID(), 3, new BigDecimal("10.50"));

        Order order = Order.fromCart(cart);

        assertEquals(cart.getTotal(), order.getTotalAmount());
        assertEquals(new BigDecimal("131.50"), order.getTotalAmount());
    }

    @Test
    void fromCart_shouldSnapshotItemFields() {
        Cart cart = Cart.create();
        UUID productId = UUID.randomUUID();
        cart.addItem(productId, 2, new BigDecimal("50.00"));

        Order order = Order.fromCart(cart);

        assertEquals(1, order.getItems().size());
        OrderItem item = order.getItems().get(0);
        assertEquals(productId, item.getProductId());
        assertEquals(2, item.getQuantity());
        assertEquals(new BigDecimal("50.00"), item.getUnitPrice());
        assertEquals(new BigDecimal("100.00"), item.getLineTotal());
    }

    @Test
    void fromCart_shouldGenerateFreshOrderItemIds() {
        Cart cart = Cart.create();
        cart.addItem(UUID.randomUUID(), 1, new BigDecimal("10.00"));
        CartItem source = cart.getItems().get(0);

        Order order = Order.fromCart(cart);

        OrderItem snapshot = order.getItems().get(0);
        assertNotNull(snapshot.getId());
        assertNotEquals(source.getId(), snapshot.getId());
    }

    @Test
    void fromCart_emptyCart_shouldYieldEmptyItemsAndZeroTotal() {
        Cart cart = Cart.create();

        Order order = Order.fromCart(cart);

        assertTrue(order.getItems().isEmpty());
        assertEquals(cart.getTotal(), order.getTotalAmount());
    }

    @Test
    void getItems_shouldReturnUnmodifiableList() {
        Cart cart = Cart.create();
        Order order = Order.fromCart(cart);

        assertThrows(UnsupportedOperationException.class, () -> order.getItems().add(null));
    }
}
