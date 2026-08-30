package com.InvitationSystem.InvitationSystem.util;

import com.InvitationSystem.InvitationSystem.entity.AdmissionType;

/**
 * Presentation helper for the card press. Layout still comes from the template;
 * this only supplies the guest-name string the overlay paints.
 */
public final class AdmissionDisplayName {

    private AdmissionDisplayName() {
    }

    public static String forGuest(String fullName, AdmissionType admissionType) {
        String name = fullName == null || fullName.isBlank() ? "Valued Guest" : fullName.trim();
        if (AdmissionType.fromNullable(admissionType) == AdmissionType.DOUBLE) {
            return name + " + 1";
        }
        return name;
    }
}
