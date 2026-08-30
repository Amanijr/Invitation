package com.InvitationSystem.InvitationSystem.service.impl;

import com.InvitationSystem.InvitationSystem.entity.DeliveryLog;
import com.InvitationSystem.InvitationSystem.repository.DeliveryLogRepository;
import com.InvitationSystem.InvitationSystem.service.DeliveryWebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class DeliveryWebhookServiceImpl implements DeliveryWebhookService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryWebhookServiceImpl.class);

    @Autowired
    private DeliveryLogRepository deliveryLogRepository;

    @Override
    @Transactional
    public void applySmsGateEvent(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return;
        }

        String event = stringValue(payload.get("event"));
        Map<String, Object> nested = nestedMap(payload.get("payload"));
        String messageId = firstNonBlank(
                nested != null ? stringValue(nested.get("messageId")) : null,
                nested != null ? stringValue(nested.get("id")) : null,
                stringValue(payload.get("messageId")));
        String state = nested != null ? stringValue(nested.get("state")) : null;

        if (messageId == null || messageId.isBlank()) {
            log.info("SMS Gate webhook ignored: missing message id");
            return;
        }

        Optional<DeliveryLog> logOpt = deliveryLogRepository.findFirstByProviderReferenceOrderBySentAtDesc(messageId);
        if (logOpt.isEmpty()) {
            log.info("SMS Gate webhook ignored: no delivery log for provider reference {}", messageId);
            return;
        }

        DeliveryLog deliveryLog = logOpt.get();
        String nextStatus = resolveStatus(event, state, deliveryLog.getStatus());
        if (nextStatus == null) {
            return;
        }

        deliveryLog.setStatus(nextStatus);
        deliveryLog.setProviderResponse("sms-gate webhook event=" + event + " state=" + state);
        if ("DELIVERED".equals(nextStatus)) {
            deliveryLog.setDeliveredAt(LocalDateTime.now());
        }
        if ("FAILED".equals(nextStatus) && nested != null) {
            String error = firstNonBlank(stringValue(nested.get("error")), stringValue(nested.get("reason")));
            if (error != null) {
                deliveryLog.setErrorMessage(error);
            }
        }
        deliveryLogRepository.save(deliveryLog);
    }

    @Override
    @Transactional
    public void applyEvolutionEvent(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return;
        }

        String event = stringValue(payload.get("event"));
        if (event != null) {
            String normalizedEvent = event.trim().toLowerCase().replace("_", ".");
            if (normalizedEvent.contains("upsert") && !normalizedEvent.contains("update")) {
                return;
            }
        }

        Map<String, Object> data = nestedMap(payload.get("data"));
        Map<String, Object> key = data != null ? nestedMap(data.get("key")) : null;
        String messageId = firstNonBlank(
                data != null ? stringValue(data.get("id")) : null,
                data != null ? stringValue(data.get("keyId")) : null,
                key != null ? stringValue(key.get("id")) : null);
        String statusToken = firstNonBlank(
                data != null ? stringValue(data.get("status")) : null,
                stringValue(payload.get("status")));

        if (messageId == null || messageId.isBlank()) {
            log.info("Evolution webhook ignored: missing message id");
            return;
        }

        Optional<DeliveryLog> logOpt = deliveryLogRepository.findFirstByProviderReferenceOrderBySentAtDesc(messageId);
        if (logOpt.isEmpty()) {
            log.info("Evolution webhook ignored: no delivery log for provider reference {}", messageId);
            return;
        }

        DeliveryLog deliveryLog = logOpt.get();
        String nextStatus = resolveEvolutionStatus(statusToken, deliveryLog.getStatus());
        if (nextStatus == null) {
            return;
        }

        deliveryLog.setStatus(nextStatus);
        deliveryLog.setProviderResponse("evolution webhook event=" + event + " status=" + statusToken);
        if ("DELIVERED".equals(nextStatus)) {
            deliveryLog.setDeliveredAt(LocalDateTime.now());
        }
        deliveryLogRepository.save(deliveryLog);
    }

    private String resolveEvolutionStatus(String statusToken, String currentStatus) {
        if (statusToken == null) {
            return null;
        }
        String normalized = statusToken.trim().toUpperCase();
        if (normalized.equals("DELIVERY_ACK") || normalized.equals("READ")
                || normalized.equals("PLAYED") || normalized.equals("DELIVERED")
                || normalized.equals("3") || normalized.equals("4") || normalized.equals("5")) {
            return "DELIVERED";
        }
        if (normalized.equals("ERROR") || normalized.equals("FAILED") || normalized.equals("0")) {
            return "FAILED";
        }
        if (normalized.equals("PENDING") || normalized.equals("SERVER_ACK")
                || normalized.equals("SENT") || normalized.equals("1") || normalized.equals("2")) {
            if ("DELIVERED".equalsIgnoreCase(currentStatus)) {
                return null;
            }
            return "SENT";
        }
        return null;
    }

    private String resolveStatus(String event, String state, String currentStatus) {
        String token = (event != null && !event.isBlank() ? event : state);
        if (token == null) {
            return null;
        }
        String normalized = token.trim().toLowerCase();
        boolean delivered = normalized.equals("sms:delivered") || normalized.equals("delivered");
        boolean failed = normalized.equals("sms:failed") || normalized.equals("failed");
        boolean sent = normalized.equals("sms:sent") || normalized.equals("sent") || normalized.equals("pending");

        if (delivered) {
            return "DELIVERED";
        }
        if (failed) {
            return "FAILED";
        }
        if (sent) {
            if ("DELIVERED".equalsIgnoreCase(currentStatus)) {
                return null;
            }
            return "SENT";
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> nestedMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank() && !"null".equals(value)) {
                return value;
            }
        }
        return null;
    }
}
