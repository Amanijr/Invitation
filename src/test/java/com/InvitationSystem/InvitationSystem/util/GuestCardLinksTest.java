package com.InvitationSystem.InvitationSystem.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GuestCardLinksTest {

    @Test
    void cardViewUrlUsesFrontendInvitePath() {
        assertEquals(
                "http://localhost:5173/invite/token-neema-777",
                GuestCardLinks.cardViewUrl("http://localhost:5173", "token-neema-777"));
    }

    @Test
    void cardViewUrlTrimsTrailingSlash() {
        assertEquals(
                "http://127.0.0.1:5173/invite/abc",
                GuestCardLinks.cardViewUrl("http://127.0.0.1:5173/", "abc"));
    }

    @Test
    void cardViewUrlEmptyWhenTokenMissing() {
        assertEquals("", GuestCardLinks.cardViewUrl("http://localhost:5173", "  "));
    }
}
