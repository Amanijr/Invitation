package com.InvitationSystem.InvitationSystem.provider.impl;

import com.InvitationSystem.InvitationSystem.provider.DeliveryRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmsChannelProviderCopyTest {

    @Test
    void smsIncludesDoorCodeAndCardLink() {
        String text = SmsChannelProvider.composeSmsText(DeliveryRequest.builder()
                .guestName("Neema Joseph")
                .eventName("Amani & Neema Gala")
                .invitationToken("token-neema-777")
                .invitationUrl("http://localhost:5173/invite/token-neema-777")
                .build());

        assertTrue(text.contains("Door code: token-neema-777"));
        assertTrue(text.contains("http://localhost:5173/invite/token-neema-777"));
        assertTrue(text.contains("Amani & Neema Gala"));
    }

    @Test
    void smsOmitsDoorCodeWhenTokenMissing() {
        String text = SmsChannelProvider.composeSmsText(DeliveryRequest.builder()
                .guestName("Neema Joseph")
                .eventName("Gala")
                .invitationUrl("http://localhost:8080/card")
                .build());

        assertFalse(text.contains("Door code:"));
        assertTrue(text.contains("Card: http://localhost:8080/card"));
    }
}
