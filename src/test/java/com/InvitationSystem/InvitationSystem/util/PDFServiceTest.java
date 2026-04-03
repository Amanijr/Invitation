package com.InvitationSystem.InvitationSystem.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class PDFServiceTest {

    private PDFService pdfService;

    @BeforeEach
    void setUp() {
        pdfService = new PDFService();
    }

     @Test
    void testGenerateReceiptPDF_Success() {
        // Arrange
        String receiptNumber = "RECEIPT-001";
        String eventName = "Wedding Reception";
        String guestName = "John Doe";
        String guestEmail = "john@example.com";
        String guestPhone = "+1234567890";
        Map<String, String> additionalData = new HashMap<>();
        additionalData.put("Location", "Grand Hall");
        additionalData.put("Date", "2026-04-15");

        // Act
        String pdfBase64 = pdfService.generateReceiptPDF(
                receiptNumber, eventName, guestName, guestEmail, guestPhone, additionalData
        );

        // Assert
        assertNotNull(pdfBase64);
        assertFalse(pdfBase64.isEmpty());
        assertTrue(pdfBase64.length() > 100);
        // Verify it's valid Base64
        assertDoesNotThrow(() -> Base64.getDecoder().decode(pdfBase64));
    }

    @Test
    void testGenerateReceiptPDF_WithoutAdditionalData() {
        // Arrange
        String receiptNumber = "RECEIPT-002";
        String eventName = "Corporate Event";
        String guestName = "Jane Smith";
        String guestEmail = "jane@example.com";
        String guestPhone = "+0987654321";

        // Act
        String pdfBase64 = pdfService.generateReceiptPDF(
                receiptNumber, eventName, guestName, guestEmail, guestPhone, null
        );

        // Assert
        assertNotNull(pdfBase64);
        assertFalse(pdfBase64.isEmpty());
        assertTrue(pdfBase64.length() > 100);
    }

    @Test
    void testGenerateReceiptPDF_WithEmptyAdditionalData() {
        // Arrange
        String receiptNumber = "RECEIPT-003";
        String eventName = "Birthday Party";
        String guestName = "Bob Johnson";
        String guestEmail = "bob@example.com";
        String guestPhone = "+5555555555";

        // Act
        String pdfBase64 = pdfService.generateReceiptPDF(
                receiptNumber, eventName, guestName, guestEmail, guestPhone, new HashMap<>()
        );

        // Assert
        assertNotNull(pdfBase64);
        assertFalse(pdfBase64.isEmpty());
        assertTrue(pdfBase64.length() > 100);
    }

    @Test
    void testGenerateReceiptPDF_WithSpecialCharacters() {
        // Arrange
        String receiptNumber = "RECEIPT-004";
        String eventName = "Event with special chars: & < > \" '";
        String guestName = "José García";
        String guestEmail = "jose@example.com";
        String guestPhone = "+3456789012";
        Map<String, String> additionalData = new HashMap<>();
        additionalData.put("Special Info", "Contains & symbols");

        // Act
        String pdfBase64 = pdfService.generateReceiptPDF(
                receiptNumber, eventName, guestName, guestEmail, guestPhone, additionalData
        );

        // Assert
        assertNotNull(pdfBase64);
        assertFalse(pdfBase64.isEmpty());
        assertTrue(pdfBase64.length() > 100);
    }

    @Test
    void testGenerateReceiptPDF_WithMultipleAdditionalFields() {
        // Arrange
        String receiptNumber = "RECEIPT-005";
        String eventName = "Large Event";
        String guestName = "Alice Wonder";
        String guestEmail = "alice@example.com";
        String guestPhone = "+1111111111";
        Map<String, String> additionalData = new HashMap<>();
        additionalData.put("Location", "New York");
        additionalData.put("Time", "6:00 PM");
        additionalData.put("Dress Code", "Formal");
        additionalData.put("Contact", "+1-800-WEDDING");

        // Act
        String pdfBase64 = pdfService.generateReceiptPDF(
                receiptNumber, eventName, guestName, guestEmail, guestPhone, additionalData
        );

        // Assert
        assertNotNull(pdfBase64);
        assertFalse(pdfBase64.isEmpty());
        assertTrue(pdfBase64.length() > 100);
        assertDoesNotThrow(() -> Base64.getDecoder().decode(pdfBase64));
    }

    @Test
    void testGenerateReceiptPDF_IsValidBase64() {
        // Arrange
        String receiptNumber = "RECEIPT-006";
        String eventName = "Test Event";
        String guestName = "Test User";
        String guestEmail = "test@example.com";
        String guestPhone = "+1234567890";

        // Act
        String pdfBase64 = pdfService.generateReceiptPDF(
                receiptNumber, eventName, guestName, guestEmail, guestPhone, null
        );

        // Assert - Verify it's valid Base64
        assertDoesNotThrow(() -> Base64.getDecoder().decode(pdfBase64));
        byte[] decodedBytes = Base64.getDecoder().decode(pdfBase64);
        assertTrue(decodedBytes.length > 0);
    }

    @Test
    void testGenerateReceiptPDF_DifferentReceiptNumbers() {
        // Arrange
        String[] receiptNumbers = {"RECEIPT-100", "INV-2026-001", "REC-ABC-123"};
        String eventName = "Event";
        String guestName = "User";
        String guestEmail = "user@example.com";
        String guestPhone = "+1234567890";

        // Act & Assert
        for (String receiptNumber : receiptNumbers) {
            String pdfBase64 = pdfService.generateReceiptPDF(
                    receiptNumber, eventName, guestName, guestEmail, guestPhone, null
            );
            assertNotNull(pdfBase64);
            assertFalse(pdfBase64.isEmpty());
        }
    }

    @Test
    void testGenerateReceiptPDF_InternationalCharacters() {
        // Arrange
        String receiptNumber = "RECEIPT-007";
        String eventName = "Célébration 🎉";
        String guestName = "François Müller";
        String guestEmail = "francois@example.com";
        String guestPhone = "+33123456789";

        // Act
        String pdfBase64 = pdfService.generateReceiptPDF(
                receiptNumber, eventName, guestName, guestEmail, guestPhone, null
        );

        // Assert
        assertNotNull(pdfBase64);
        assertFalse(pdfBase64.isEmpty());
    }

    @Test
    void testGenerateReceiptPDF_LongEventName() {
        // Arrange
        String receiptNumber = "RECEIPT-008";
        String eventName = "Very Important Corporate Conference and Networking Event 2026";
        String guestName = "John Executive";
        String guestEmail = "john.exec@company.com";
        String guestPhone = "+1-800-555-0123";

        // Act
        String pdfBase64 = pdfService.generateReceiptPDF(
                receiptNumber, eventName, guestName, guestEmail, guestPhone, null
        );

        // Assert
        assertNotNull(pdfBase64);
        assertFalse(pdfBase64.isEmpty());
        assertTrue(pdfBase64.length() > 100);
    }

    @Test
    void testGenerateReceiptPDF_AllFieldsPopulated() {
        // Arrange
        String receiptNumber = "RCP-2026-00001";
        String eventName = "Golden Anniversary Celebration";
        String guestName = "Margaret Elizabeth Thompson";
        String guestEmail = "margaret.thompson@email.com";
        String guestPhone = "+1-555-123-4567";
        Map<String, String> additionalData = new HashMap<>();
        additionalData.put("Venue", "Grand Ballroom, Downtown Hotel");
        additionalData.put("Date", "April 15, 2026");
        additionalData.put("Time", "6:00 PM - 11:00 PM");
        additionalData.put("Dress Code", "Black Tie Optional");
        additionalData.put("RSVP By", "April 1, 2026");
        additionalData.put("Dietary Restrictions", "None");
        additionalData.put("Plus One Allowed", "Yes");

        // Act
        String pdfBase64 = pdfService.generateReceiptPDF(
                receiptNumber, eventName, guestName, guestEmail, guestPhone, additionalData
        );

        // Assert
        assertNotNull(pdfBase64);
        assertFalse(pdfBase64.isEmpty());
        assertTrue(pdfBase64.length() > 500); // Should be larger with many fields
        assertDoesNotThrow(() -> Base64.getDecoder().decode(pdfBase64));
    }

    @Test
    void testPDFServiceNotNull() {
        // Assert
        assertNotNull(pdfService);
    }

    @Test
    void testGenerateReceiptPDF_WithNumericData() {
        // Arrange
        String receiptNumber = "RECEIPT-2026-001";
        String eventName = "Annual Gala";
        String guestName = "David Chen";
        String guestEmail = "david.chen@example.com";
        String guestPhone = "+86-10-1234-5678";
        Map<String, String> additionalData = new HashMap<>();
        additionalData.put("Ticket Price", "$150.00");
        additionalData.put("Table Number", "12");
        additionalData.put("Guest Count", "2");

        // Act
        String pdfBase64 = pdfService.generateReceiptPDF(
                receiptNumber, eventName, guestName, guestEmail, guestPhone, additionalData
        );

        // Assert
        assertNotNull(pdfBase64);
        assertFalse(pdfBase64.isEmpty());
        assertTrue(pdfBase64.length() > 100);
    }
}
