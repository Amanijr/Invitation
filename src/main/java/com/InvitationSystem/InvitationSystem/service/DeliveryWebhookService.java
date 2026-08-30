package com.InvitationSystem.InvitationSystem.service;

import java.util.Map;

public interface DeliveryWebhookService {

    void applySmsGateEvent(Map<String, Object> payload);

    void applyEvolutionEvent(Map<String, Object> payload);
}
