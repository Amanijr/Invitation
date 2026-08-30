package com.InvitationSystem.InvitationSystem.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TokenGeneratorServiceTest {

    private final TokenGeneratorService tokenGeneratorService = new TokenGeneratorService();

    @Test
    void generateSecureToken_NonEmptyAndUnique() {
        String token1 = tokenGeneratorService.generateSecureToken();
        String token2 = tokenGeneratorService.generateSecureToken();

        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
        assertTrue(token1.length() >= 32);
    }

    @Test
    void generateSecureToken_UniquenessAcrossMultipleGenerations() {
        Set<String> tokens = new HashSet<>();
        int count = 1000;
        for (int i = 0; i < count; i++) {
            tokens.add(tokenGeneratorService.generateSecureToken());
        }
        assertEquals(count, tokens.size(), "All generated tokens must be unique (no collisions)");
    }
}
