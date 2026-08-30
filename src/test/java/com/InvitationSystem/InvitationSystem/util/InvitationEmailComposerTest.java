package com.InvitationSystem.InvitationSystem.util;

import com.InvitationSystem.InvitationSystem.provider.DeliveryRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvitationEmailComposerTest {

    @Test
    void compose_includesGuestEventWhenAndWhere_andSkipsApiLinks() {
        DeliveryRequest request = DeliveryRequest.builder()
                .guestName("Neema Mwamba")
                .eventName("Gold wedding")
                .eventDate("2026-09-06 18:00")
                .venue("Dar es Salaam")
                .invitationUrl("http://localhost:8080/api/v1/invitations/token/secret")
                .build();

        String subject = InvitationEmailComposer.subject(request);
        String plain = InvitationEmailComposer.plainText(request);
        String html = InvitationEmailComposer.html(request, true);

        assertTrue(subject.contains("Gold wedding"));
        assertTrue(plain.contains("Hello Neema Mwamba"));
        assertTrue(plain.contains("You are invited to Gold wedding"));
        assertTrue(plain.contains("When: 2026-09-06 18:00"));
        assertTrue(plain.contains("Where: Dar es Salaam"));
        assertTrue(plain.contains("QR is for the door"));
        assertFalse(plain.contains("localhost"));
        assertFalse(plain.contains("/api/"));

        assertTrue(html.contains("Neema Mwamba"));
        assertTrue(html.contains("cid:invitation-card"));
        assertFalse(html.contains("localhost"));
        assertFalse(html.contains("template_title"));
    }
}
