package com.cart.checkout.domain.payment;

import com.cart.checkout.exceptions.InvalidPaymentTransitionException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PaymentTest {

    @Test
    void initiate_shouldStartInInitiatedStatusWithNoProviderId() {
        UUID orderId = UUID.randomUUID();

        Payment payment = Payment.initiate(orderId, new BigDecimal("100.00"));

        assertEquals(PaymentStatus.INITIATED, payment.getStatus());
        assertNull(payment.getProviderPaymentId());
        assertEquals(orderId, payment.getOrderId());
        assertEquals(new BigDecimal("100.00"), payment.getAmount());
    }

    @Test
    void initiate_shouldGenerateFreshPaymentId() {
        Payment a = Payment.initiate(UUID.randomUUID(), new BigDecimal("10.00"));
        Payment b = Payment.initiate(UUID.randomUUID(), new BigDecimal("10.00"));

        assertNotNull(a.getId());
        assertNotNull(b.getId());
        assertNotEquals(a.getId(), b.getId());
    }

    @Test
    void attachProviderId_fromInitiated_shouldStoreProviderId() {
        Payment payment = newInitiated();

        payment.attachProviderId("provider-123");

        assertEquals("provider-123", payment.getProviderPaymentId());
    }

    @Test
    void attachProviderId_whenAlreadySet_shouldThrow() {
        Payment payment = newInitiated();
        payment.attachProviderId("provider-123");

        assertThrows(InvalidPaymentTransitionException.class,
                () -> payment.attachProviderId("provider-456"));
    }

    @Test
    void markConfirmed_fromInitiatedWithProviderId_shouldTransitionToConfirmed() {
        Payment payment = newInitiated();
        payment.attachProviderId("provider-123");

        payment.markConfirmed();

        assertEquals(PaymentStatus.CONFIRMED, payment.getStatus());
    }

    @Test
    void markConfirmed_withoutProviderId_shouldThrow() {
        Payment payment = newInitiated();

        assertThrows(InvalidPaymentTransitionException.class, payment::markConfirmed);
        assertEquals(PaymentStatus.INITIATED, payment.getStatus());
    }

    @Test
    void markConfirmed_fromConfirmed_shouldThrow() {
        Payment payment = newConfirmed();

        assertThrows(InvalidPaymentTransitionException.class, payment::markConfirmed);
    }

    @Test
    void markConfirmed_fromFailed_shouldThrow() {
        Payment payment = newFailed();

        assertThrows(InvalidPaymentTransitionException.class, payment::markConfirmed);
    }

    @Test
    void markFailed_fromInitiatedWithProviderId_shouldTransitionToFailed() {
        Payment payment = newInitiated();
        payment.attachProviderId("provider-123");

        payment.markFailed();

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
    }

    @Test
    void markFailed_withoutProviderId_shouldThrow() {
        Payment payment = newInitiated();

        assertThrows(InvalidPaymentTransitionException.class, payment::markFailed);
        assertEquals(PaymentStatus.INITIATED, payment.getStatus());
    }

    @Test
    void markFailed_fromConfirmed_shouldThrow() {
        Payment payment = newConfirmed();

        assertThrows(InvalidPaymentTransitionException.class, payment::markFailed);
    }

    @Test
    void markFailed_fromFailed_shouldThrow() {
        Payment payment = newFailed();

        assertThrows(InvalidPaymentTransitionException.class, payment::markFailed);
    }

    @Test
    void isTerminal_initiated_shouldBeFalse() {
        assertFalse(newInitiated().isTerminal());
    }

    @Test
    void isTerminal_confirmed_shouldBeTrue() {
        assertTrue(newConfirmed().isTerminal());
    }

    @Test
    void isTerminal_failed_shouldBeTrue() {
        assertTrue(newFailed().isTerminal());
    }

    private static Payment newInitiated() {
        return Payment.initiate(UUID.randomUUID(), new BigDecimal("100.00"));
    }

    private static Payment newConfirmed() {
        Payment payment = newInitiated();
        payment.attachProviderId("provider-" + UUID.randomUUID());
        payment.markConfirmed();
        return payment;
    }

    private static Payment newFailed() {
        Payment payment = newInitiated();
        payment.attachProviderId("provider-" + UUID.randomUUID());
        payment.markFailed();
        return payment;
    }
}
