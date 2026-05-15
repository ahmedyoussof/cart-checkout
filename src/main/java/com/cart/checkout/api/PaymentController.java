package com.cart.checkout.api;

import com.cart.checkout.api.dto.PaymentStartResponse;
import com.cart.checkout.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/orders/{orderId}/payment/start")
    public ResponseEntity<PaymentStartResponse> startPayment(@PathVariable UUID orderId) {
        var payment = paymentService.startPayment(orderId);
        return ResponseEntity.ok(PaymentStartResponse.from(payment));
    }
}
