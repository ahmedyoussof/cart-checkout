package com.cart.checkout.domain.cart;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Entity
@Table(name = "cart_items")
public class CartItem {

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

    protected CartItem() {
    }

    CartItem(UUID productId, int quantity, BigDecimal unitPrice) {
        if (productId == null) {
            throw new IllegalArgumentException("productId is required");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new IllegalArgumentException("unitPrice must be non-negative");
        }
        this.id = UUID.randomUUID();
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice.setScale(2, RoundingMode.UNNECESSARY);
        this.lineTotal = this.unitPrice.multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.UNNECESSARY);
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
