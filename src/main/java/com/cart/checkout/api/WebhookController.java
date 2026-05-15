package com.cart.checkout.api;

import com.cart.checkout.api.dto.WebhookPayload;
import com.cart.checkout.service.WebhookService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> receive(@Valid @RequestBody WebhookPayload payload) {
        webhookService.handle(payload);
        return ResponseEntity.ok().build();
    }
}
