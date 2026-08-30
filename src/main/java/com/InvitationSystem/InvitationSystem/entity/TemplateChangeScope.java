package com.InvitationSystem.InvitationSystem.entity;

@io.swagger.v3.oas.annotations.media.Schema(description = "How far an event template change is applied")
public enum TemplateChangeScope {
    NEW_GUESTS_ONLY,
    UNSENT_INVITATIONS,
    ALL_INVITATIONS
}
