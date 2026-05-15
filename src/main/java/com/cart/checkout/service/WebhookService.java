package com.cart.checkout.service;

import com.cart.checkout.api.dto.WebhookPayload;
import com.cart.checkout.domain.order.Order;
import com.cart.checkout.domain.payment.Payment;
import com.cart.checkout.exceptions.ResourceNotFoundException;
import com.cart.checkout.repository.OrderRepository;
import com.cart.checkout.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    public WebhookService(PaymentRepository paymentRepository, OrderRepository orderRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public void handle(WebhookPayload payload) {
        Payment payment = paymentRepository.findById(payload.paymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + payload.paymentId()));

        if (payment.isTerminal()) {
            log.info("Webhook ignored (idempotent) for payment {} already in terminal status {}",
                    payment.getId(), payment.getStatus());
            return;
        }

        Order order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + payment.getOrderId()));

        switch (payload.outcome()) {
            case CONFIRMED -> {
                payment.markConfirmed();
                order.markPaid();
            }
            case FAILED -> {
                payment.markFailed();
                order.markPaymentFailed();
            }
        }

        paymentRepository.save(payment);
        orderRepository.save(order);

        log.info("Webhook applied: payment {} -> {}, order {} -> {}",
                payment.getId(), payment.getStatus(), order.getId(), order.getStatus());
    }
}
