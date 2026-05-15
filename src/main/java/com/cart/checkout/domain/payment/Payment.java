package com.cart.checkout.domain.payment;

import com.cart.checkout.exceptions.InvalidPaymentTransitionException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(nullable = false, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "provider_payment_id")
    private String providerPaymentId;

    @Version
    private Long version;

    protected Payment() {
    }

    private Payment(UUID id, UUID orderId, BigDecimal amount, PaymentStatus status) {
        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
    }

    public static Payment initiate(UUID orderId, BigDecimal amount) {
        return new Payment(UUID.randomUUID(), orderId, amount, PaymentStatus.INITIATED);
    }

    public void attachProviderId(String providerPaymentId) {
        if (this.providerPaymentId != null) {
            throw new InvalidPaymentTransitionException(
                    "Payment " + id + " already has a provider id assigned");
        }
        if (status != PaymentStatus.INITIATED) {
            throw new InvalidPaymentTransitionException(
                    "Payment " + id + " cannot attach provider id in status " + status);
        }
        this.providerPaymentId = providerPaymentId;
    }

    public void markConfirmed() {
        if (status != PaymentStatus.INITIATED) {
            throw new InvalidPaymentTransitionException(
                    "Payment " + id + " cannot transition from " + status + " to CONFIRMED");
        }
        if (providerPaymentId == null) {
            throw new InvalidPaymentTransitionException(
                    "Payment " + id + " has no provider id and cannot be confirmed");
        }
        status = PaymentStatus.CONFIRMED;
    }

    public void markFailed() {
        if (status != PaymentStatus.INITIATED) {
            throw new InvalidPaymentTransitionException(
                    "Payment " + id + " cannot transition from " + status + " to FAILED");
        }
        if (providerPaymentId == null) {
            throw new InvalidPaymentTransitionException(
                    "Payment " + id + " has no provider id and cannot be failed");
        }
        status = PaymentStatus.FAILED;
    }

    public boolean isTerminal() {
        return status == PaymentStatus.CONFIRMED || status == PaymentStatus.FAILED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getProviderPaymentId() {
        return providerPaymentId;
    }

    public Long getVersion() {
        return version;
    }
}
