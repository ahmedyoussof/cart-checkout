package com.cart.checkout.domain.payment.port;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentProvider {

    ProviderInitiationResult initiatePayment(UUID paymentId, BigDecimal amount);
}
