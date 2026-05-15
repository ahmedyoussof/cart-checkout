package com.cart.checkout.infrastructure;

import com.cart.checkout.domain.payment.port.PaymentProvider;
import com.cart.checkout.domain.payment.port.ProviderInitiationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MockPaymentProvider implements PaymentProvider {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentProvider.class);

    private final ConcurrentHashMap<UUID, String> providerIdByPaymentId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, UUID> paymentIdByProviderId = new ConcurrentHashMap<>();

    @Override
    public ProviderInitiationResult initiatePayment(UUID paymentId, BigDecimal amount) {
        String providerPaymentId = UUID.randomUUID().toString();
        providerIdByPaymentId.put(paymentId, providerPaymentId);
        paymentIdByProviderId.put(providerPaymentId, paymentId);
        log.info("Mock provider issued providerPaymentId={} for paymentId={} amount={}",
                providerPaymentId, paymentId, amount);
        return new ProviderInitiationResult(providerPaymentId);
    }

    public Optional<UUID> lookupPaymentId(String providerPaymentId) {
        return Optional.ofNullable(paymentIdByProviderId.get(providerPaymentId));
    }
}
