package com.InvitationSystem.InvitationSystem.entity;

@io.swagger.v3.oas.annotations.media.Schema(description = "Guest RSVP on a named invitation")
public enum RsvpStatus {
    NO_REPLY,
    GOING,
    NOT_GOING,
    MAYBE
}
