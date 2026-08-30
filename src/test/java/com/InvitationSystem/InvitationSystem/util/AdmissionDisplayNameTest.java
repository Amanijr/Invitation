package com.InvitationSystem.InvitationSystem.util;

import com.InvitationSystem.InvitationSystem.entity.AdmissionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdmissionDisplayNameTest {

    @Test
    void singleKeepsTheGuestName() {
        assertEquals("John Mwita", AdmissionDisplayName.forGuest("John Mwita", AdmissionType.SINGLE));
    }

    @Test
    void doubleAddsCompanionMarker() {
        assertEquals("Mary Joseph + 1", AdmissionDisplayName.forGuest("Mary Joseph", AdmissionType.DOUBLE));
    }
}
