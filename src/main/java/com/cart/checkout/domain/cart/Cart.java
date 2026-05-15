package com.cart.checkout.domain.cart;

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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "carts")
public class Cart {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CartStatus status;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "cart_id")
    private List<CartItem> items = new ArrayList<>();

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal total;

    @Version
    private Long version;

    protected Cart() {
    }

    private Cart(UUID id, CartStatus status, BigDecimal total) {
        this.id = id;
        this.status = status;
        this.total = total;
    }

    public static Cart create() {
        return new Cart(UUID.randomUUID(), CartStatus.OPEN, BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY));
    }

    public void lock() {
        if (status == CartStatus.LOCKED) {
            return;
        }
        status = CartStatus.LOCKED;
    }

    public UUID getId() {
        return id;
    }

    public CartStatus getStatus() {
        return status;
    }

    public List<CartItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public BigDecimal getTotal() {
        return total;
    }

    public Long getVersion() {
        return version;
    }
}
