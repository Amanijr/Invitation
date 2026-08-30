package com.InvitationSystem.InvitationSystem.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Supported dynamic fields for template positioning")
public enum FieldType {
    GUEST_NAME,
    EVENT_NAME,
    EVENT_DATE,
    EVENT_TIME,
    EVENT_VENUE,
    QR_CODE
}
