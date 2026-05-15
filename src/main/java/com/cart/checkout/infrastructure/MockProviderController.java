package com.cart.checkout.infrastructure;

import com.cart.checkout.api.dto.WebhookPayload;
import com.cart.checkout.exceptions.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/mock-provider")
public class MockProviderController {

    private static final Logger log = LoggerFactory.getLogger(MockProviderController.class);

    private final MockPaymentProvider provider;
    private final MockProviderClient client;

    public MockProviderController(MockPaymentProvider provider, MockProviderClient client) {
        this.provider = provider;
        this.client = client;
    }

    @PostMapping("/{providerPaymentId}/trigger")
    public ResponseEntity<Void> trigger(
            @PathVariable String providerPaymentId,
            @RequestParam WebhookPayload.Outcome outcome) {
        UUID paymentId = provider.lookupPaymentId(providerPaymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Unknown providerPaymentId: " + providerPaymentId));
        log.info("Mock provider trigger received providerPaymentId={} paymentId={} outcome={}",
                providerPaymentId, paymentId, outcome);
        client.sendWebhook(new WebhookPayload(paymentId, providerPaymentId, outcome));
        return ResponseEntity.ok().build();
    }
}
