package com.InvitationSystem.InvitationSystem.entity;

@io.swagger.v3.oas.annotations.media.Schema(description = "Invitation lifecycle statuses")
public enum InvitationStatus {
    GENERATED,
    SENT,
    DELIVERED,
    OPENED,
    USED,
    EXPIRED,
    FAILED
}
