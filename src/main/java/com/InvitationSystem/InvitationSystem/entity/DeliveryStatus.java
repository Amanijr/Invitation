package com.InvitationSystem.InvitationSystem.entity;

@io.swagger.v3.oas.annotations.media.Schema(description = "Delivery channel statuses")
public enum DeliveryStatus {
    PENDING,
    PROCESSING,
    SENT,
    SENT_EMAIL,
    SENT_WHATSAPP,
    DELIVERED,
    FAILED
}
