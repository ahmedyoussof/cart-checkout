package com.cart.checkout.service;

import com.cart.checkout.domain.order.Order;
import com.cart.checkout.domain.payment.Payment;
import com.cart.checkout.domain.payment.port.PaymentProvider;
import com.cart.checkout.domain.payment.port.ProviderInitiationResult;
import com.cart.checkout.exceptions.ResourceNotFoundException;
import com.cart.checkout.repository.OrderRepository;
import com.cart.checkout.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentProvider paymentProvider;

    public PaymentService(OrderRepository orderRepository,
                          PaymentRepository paymentRepository,
                          PaymentProvider paymentProvider) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.paymentProvider = paymentProvider;
    }

    @Transactional
    public Payment startPayment(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        order.markPendingPayment();

        Payment payment = paymentRepository.save(Payment.initiate(orderId, order.getTotalAmount()));
        ProviderInitiationResult result = paymentProvider.initiatePayment(payment.getId(), payment.getAmount());
        payment.attachProviderId(result.providerPaymentId());

        Payment saved = paymentRepository.save(payment);
        orderRepository.save(order);

        log.info("Started payment {} for order {} (providerPaymentId={})",
                saved.getId(), orderId, saved.getProviderPaymentId());
        return saved;
    }
}
