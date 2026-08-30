package com.InvitationSystem.InvitationSystem.controller;

import com.InvitationSystem.InvitationSystem.service.DeliveryWebhookService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Hidden
@RestController
@RequestMapping("/api/v1/webhooks")
public class DeliveryWebhookController {

    @Autowired
    private DeliveryWebhookService deliveryWebhookService;

    @Value("${delivery.sms.android.webhook-secret:}")
    private String smsGateWebhookSecret;

    @Value("${delivery.whatsapp.evolution.webhook-secret:}")
    private String evolutionWebhookSecret;

    @PostMapping("/sms-gate")
    public ResponseEntity<Void> smsGate(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String webhookSecret,
            @RequestBody(required = false) Map<String, Object> payload) {
        if (smsGateWebhookSecret != null && !smsGateWebhookSecret.isBlank()) {
            if (webhookSecret == null || !smsGateWebhookSecret.equals(webhookSecret)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }
        deliveryWebhookService.applySmsGateEvent(payload);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/evolution")
    public ResponseEntity<Void> evolution(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String webhookSecret,
            @RequestHeader(value = "apikey", required = false) String apiKey,
            @RequestBody(required = false) Map<String, Object> payload) {
        if (evolutionWebhookSecret != null && !evolutionWebhookSecret.isBlank()) {
            String provided = webhookSecret != null && !webhookSecret.isBlank() ? webhookSecret : apiKey;
            if (provided == null || !evolutionWebhookSecret.equals(provided)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }
        deliveryWebhookService.applyEvolutionEvent(payload);
        return ResponseEntity.ok().build();
    }
}
