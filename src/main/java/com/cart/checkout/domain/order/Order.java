package com.cart.checkout.domain.order;

import com.cart.checkout.domain.cart.Cart;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    private UUID id;

    @Column(name = "cart_id", nullable = false)
    private UUID cartId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id")
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "total_amount", nullable = false, scale = 2)
    private BigDecimal totalAmount;

    @Version
    private Long version;

    protected Order() {
    }

    private Order(UUID id, UUID cartId, OrderStatus status, BigDecimal totalAmount) {
        this.id = id;
        this.cartId = cartId;
        this.status = status;
        this.totalAmount = totalAmount;
    }

    public static Order fromCart(Cart cart) {
        Order order = new Order(UUID.randomUUID(), cart.getId(), OrderStatus.CREATED, cart.getTotal());
        cart.getItems().forEach(ci -> order.items.add(new OrderItem(ci)));
        return order;
    }

    public void markPendingPayment() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void markPaid() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void markPaymentFailed() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public UUID getId() {
        return id;
    }

    public UUID getCartId() {
        return cartId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Long getVersion() {
        return version;
    }
}
