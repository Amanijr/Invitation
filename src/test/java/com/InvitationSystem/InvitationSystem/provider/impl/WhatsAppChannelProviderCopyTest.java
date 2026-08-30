package com.InvitationSystem.InvitationSystem.provider.impl;

import com.InvitationSystem.InvitationSystem.provider.DeliveryRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WhatsAppChannelProviderCopyTest {

    @Test
    void whatsAppIncludesVenueAndTime() {
        String text = WhatsAppChannelProvider.composeWhatsAppText(DeliveryRequest.builder()
                .guestName("Neema Joseph")
                .eventName("Amani & Neema")
                .eventDate("2026-09-12 18:00")
                .venue("The Slipway, Dar es Salaam")
                .invitationUrl("http://localhost:5173/invite/token-neema")
                .build());

        assertTrue(text.contains("Amani & Neema"));
        assertTrue(text.contains("When: *12 September 2026, 6:00 PM*"));
        assertTrue(text.contains("Where: *The Slipway, Dar es Salaam*"));
        assertTrue(text.contains("http://localhost:5173/invite/token-neema"));
    }

    @Test
    void whatsAppOmitsPlaceholderWhenAndWhere() {
        String text = WhatsAppChannelProvider.composeWhatsAppText(DeliveryRequest.builder()
                .guestName("Neema Joseph")
                .eventName("Gala")
                .eventDate("TBD")
                .venue("Venue")
                .build());

        assertFalse(text.contains("When:"));
        assertFalse(text.contains("Where:"));
    }
}
