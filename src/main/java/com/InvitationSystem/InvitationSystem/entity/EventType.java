package com.InvitationSystem.InvitationSystem.entity;

/**
 * Enum representing different types of events.
 * Each event in the system must be categorized with one of these types.
 */
@io.swagger.v3.oas.annotations.media.Schema(description = "Supported event types")
public enum EventType {
    WEDDING,
    CONFERENCE,
    BIRTHDAY,
    FUNERAL,
    CORPORATE,
    OTHER
}
