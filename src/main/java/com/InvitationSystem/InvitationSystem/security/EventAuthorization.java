package com.InvitationSystem.InvitationSystem.security;

import com.InvitationSystem.InvitationSystem.entity.Event;
import com.InvitationSystem.InvitationSystem.entity.UserRole;
import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

public final class EventAuthorization {

    private EventAuthorization() {
    }

    public static void requireAdministrator(UserRole role) {
        if (role == null || role == UserRole.GUEST) {
            throw new AccessDeniedException("Not authorized to manage invitations for this event");
        }
    }

    public static void requireEventOwnerOrAdmin(Event event, UUID actorId, UserRole role) {
        requireAdministrator(role);
        if (role == UserRole.ADMIN) {
            return;
        }
        if (event == null || actorId == null || !actorId.equals(event.getCreatedBy())) {
            throw new AccessDeniedException("Not authorized to manage this event");
        }
    }
}
