package com.InvitationSystem.InvitationSystem.util;

import java.util.regex.Pattern;

public class PhoneNormalizationUtil {

    private static final Pattern E164_PATTERN = Pattern.compile("^\\+[1-9]\\d{6,14}$");

    /**
     * Normalize a raw phone number string to E.164 international standard format.
     * Example:
     *   "+255 712-345 678" -> "+255712345678"
     *   "0712345678"       -> "+255712345678" (assuming local East African default if starting with 0)
     *   "255712345678"      -> "+255712345678"
     */
    public static String normalizePhoneNumber(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return null;
        }

        String cleaned = rawPhone.trim().replaceAll("[\\s()\\-.]", "");

        if (cleaned.startsWith("00")) {
            cleaned = "+" + cleaned.substring(2);
        } else if (cleaned.startsWith("0") && cleaned.length() == 10) {
            // Local 10-digit number format (e.g. 07xx xxx xxx) -> convert to +255...
            cleaned = "+255" + cleaned.substring(1);
        } else if (!cleaned.startsWith("+")) {
            cleaned = "+" + cleaned;
        }

        return cleaned;
    }

    /**
     * Validate whether a normalized phone number satisfies E.164 format.
     */
    public static boolean isValidE164(String phone) {
        if (phone == null || phone.isBlank()) {
            return false;
        }
        return E164_PATTERN.matcher(phone).matches();
    }
}
