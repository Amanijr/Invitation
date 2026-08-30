package com.InvitationSystem.InvitationSystem.entity;

@io.swagger.v3.oas.annotations.media.Schema(description = "Derived door-scan entitlement state")
public enum CheckInEntitlementState {
    VALID,
    PARTIALLY_USED,
    FULLY_USED,
    INVALID,
    REVOKED
}
