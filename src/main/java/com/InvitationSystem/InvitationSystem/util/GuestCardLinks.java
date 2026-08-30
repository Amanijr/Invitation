package com.InvitationSystem.InvitationSystem.util;

/**
 * Guest-facing card view on the web desk, not the JSON API.
 */
public final class GuestCardLinks {

    private GuestCardLinks() {}

    public static String cardViewUrl(String publicBaseUrl, String uniqueToken) {
        if (uniqueToken == null || uniqueToken.isBlank()) {
            return "";
        }
        String base = publicBaseUrl == null ? "" : publicBaseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.isEmpty()) {
            base = "http://localhost:5173";
        }
        return base + "/invite/" + uniqueToken.trim();
    }
}
