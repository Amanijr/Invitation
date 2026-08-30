package com.InvitationSystem.InvitationSystem.util;

import com.InvitationSystem.InvitationSystem.entity.Template;

import java.util.UUID;

public final class TemplateAvailability {

    public static final UUID LIBRARY_EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private TemplateAvailability() {
    }

    public static boolean isAvailableForEvent(Template template, UUID eventId) {
        return template != null && template.isActive();
    }
}
