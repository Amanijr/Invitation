package com.InvitationSystem.InvitationSystem.util;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

@Service
public class TokenGeneratorService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    /**
     * Generates a cryptographically secure 256-bit URL-safe token.
     * Guaranteed high entropy and non-predictability.
     */
    public String generateSecureToken() {
        byte[] randomBytes = new byte[32]; // 256 bits of entropy
        SECURE_RANDOM.nextBytes(randomBytes);
        return URL_ENCODER.encodeToString(randomBytes);
    }

    /**
     * Fallback standard UUID v4 based on SecureRandom.
     */
    public String generateSecureUuidToken() {
        return UUID.randomUUID().toString();
    }
}
