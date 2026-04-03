package com.InvitationSystem.InvitationSystem.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class QRCodeServiceTest {

    private QRCodeService qrCodeService;

    @BeforeEach
    void setUp() {
        qrCodeService = new QRCodeService();
    }

    @Test
    void testGenerateQRCodeImage_Success() {
        // Arrange
        String qrData = "https://example.com/invite?token=test123";
        // Act
        String qrCodeBase64 = qrCodeService.generateQRCodeImage(qrData);

        // Assert
        assertNotNull(qrCodeBase64);
        assertFalse(qrCodeBase64.isEmpty());
        // Verify it's valid Base64
        assertDoesNotThrow(() -> Base64.getDecoder().decode(qrCodeBase64));
    }

    @Test
    void testGenerateQRCodeImage_WithShortData() {
        // Arrange
        String qrData = "A";

        // Act
        String qrCodeBase64 = qrCodeService.generateQRCodeImage(qrData);

        // Assert
        assertNotNull(qrCodeBase64);
        assertFalse(qrCodeBase64.isEmpty());
    }

    @Test
    void testGenerateQRCodeImage_WithLongData() {
        // Arrange
        StringBuilder longData = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longData.append("https://example.com/invite?token=test");
        }

        // Act & Assert
        assertThrows(RuntimeException.class, () -> qrCodeService.generateQRCodeImage(longData.toString()));
    }

    @Test
    void testGenerateQRCodeImage_WithSpecialCharacters() {
        // Arrange
        String qrData = "https://example.com/invite?token=test@#$%^&*()";

        // Act
        String qrCodeBase64 = qrCodeService.generateQRCodeImage(qrData);

        // Assert
        assertNotNull(qrCodeBase64);
        assertFalse(qrCodeBase64.isEmpty());
    }

    @Test
    void testGenerateInvitationQRCode_Success() {
        // Arrange
        String token = "unique-token-123";
        String baseUrl = "http://localhost:8080";

        // Act
        String qrCodeBase64 = qrCodeService.generateInvitationQRCode(token, baseUrl);

        // Assert
        assertNotNull(qrCodeBase64);
        assertFalse(qrCodeBase64.isEmpty());
        // Verify it's valid Base64
        assertDoesNotThrow(() -> Base64.getDecoder().decode(qrCodeBase64));
    }

    @Test
    void testGenerateInvitationQRCode_WithDifferentBaseUrls() {
        // Arrange
        String token = "unique-token-123";
        String[] baseUrls = {
                "http://localhost:8080",
                "https://invitations.com",
                "https://api.example.org"
        };

        // Act & Assert
        for (String baseUrl : baseUrls) {
            String qrCodeBase64 = qrCodeService.generateInvitationQRCode(token, baseUrl);
            assertNotNull(qrCodeBase64);
            assertFalse(qrCodeBase64.isEmpty());
        }
    }

    @Test
    void testGenerateQRCodeImage_EmptyString_ThrowsException() {
        // Act & Assert
        assertThrows(RuntimeException.class, () -> qrCodeService.generateQRCodeImage(""));
    }

    @Test
    void testQRCodeImageSize() {
        // Arrange
        String qrData = "https://example.com/invite?token=test123";

        // Act
        String qrCodeBase64 = qrCodeService.generateQRCodeImage(qrData);
        byte[] decoded = Base64.getDecoder().decode(qrCodeBase64);

        // Assert
        // QR code should be a PNG file
        assertTrue(decoded.length > 0);
    }
}
