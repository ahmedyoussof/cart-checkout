package com.cart.checkout.domain.order;

import com.cart.checkout.domain.cart.CartItem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "line_total", nullable = false, scale = 2)
    private BigDecimal lineTotal;

    protected OrderItem() {
    }

    OrderItem(CartItem source) {
        this.id = UUID.randomUUID();
        this.productId = source.getProductId();
        this.quantity = source.getQuantity();
        this.unitPrice = source.getUnitPrice();
        this.lineTotal = source.getLineTotal();
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }
}
