package com.InvitationSystem.InvitationSystem.provider.impl;

import com.InvitationSystem.InvitationSystem.entity.DeliveryChannel;
import com.InvitationSystem.InvitationSystem.entity.DeliveryStatus;
import com.InvitationSystem.InvitationSystem.provider.ChannelProvider;
import com.InvitationSystem.InvitationSystem.provider.DeliveryRequest;
import com.InvitationSystem.InvitationSystem.provider.DeliveryResult;
import com.InvitationSystem.InvitationSystem.util.PhoneNormalizationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

@Component
public class WhatsAppChannelProvider implements ChannelProvider {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppChannelProvider.class);

    @Value("${delivery.whatsapp.provider:meta}")
    private String providerMode;

    @Value("${delivery.whatsapp.api-url:${WHATSAPP_API_URL:https://graph.facebook.com/v18.0/me/messages}}")
    private String whatsappApiUrl;

    @Value("${delivery.whatsapp.access-token:${WHATSAPP_ACCESS_TOKEN:test_access_token_placeholder}}")
    private String accessToken;

    @Value("${delivery.whatsapp.from-number:${WHATSAPP_FROM_NUMBER:+15005550006}}")
    private String fromNumber;

    @Value("${delivery.whatsapp.phone-number-id:${WHATSAPP_PHONE_NUMBER_ID:}}")
    private String phoneNumberId;

    @Value("${delivery.whatsapp.evolution.base-url:http://127.0.0.1:8081}")
    private String evolutionBaseUrl;

    @Value("${delivery.whatsapp.evolution.instance:}")
    private String evolutionInstance;

    @Value("${delivery.whatsapp.evolution.api-key:}")
    private String evolutionApiKey;

    private HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public DeliveryChannel getChannel() {
        return DeliveryChannel.WHATSAPP;
    }

    @Override
    public DeliveryResult send(DeliveryRequest request) {
        String rawPhone = request.getRecipientPhone();

        if (rawPhone == null || rawPhone.isBlank()) {
            log.warn("WhatsApp delivery rejected: Recipient phone number is missing or empty for invitation ID: {}", request.getInvitationId());
            return DeliveryResult.builder()
                    .success(false)
                    .status(DeliveryStatus.FAILED)
                    .recipientContact(rawPhone)
                    .errorMessage("Recipient phone number is missing or empty for WhatsApp delivery")
                    .build();
        }

        String normalizedPhone = PhoneNormalizationUtil.normalizePhoneNumber(rawPhone);
        if (!PhoneNormalizationUtil.isValidE164(normalizedPhone)) {
            log.warn("WhatsApp delivery rejected: Invalid E.164 phone format '{}' (raw: '{}') for invitation ID: {}", normalizedPhone, rawPhone, request.getInvitationId());
            return DeliveryResult.builder()
                    .success(false)
                    .status(DeliveryStatus.FAILED)
                    .recipientContact(rawPhone)
                    .errorMessage("Invalid recipient phone number format for WhatsApp: " + rawPhone)
                    .build();
        }

        // Meta WhatsApp Cloud API expects recipient phone without leading '+' (digits only)
        String waPhoneDigits = normalizedPhone.startsWith("+") ? normalizedPhone.substring(1) : normalizedPhone;

        if ("mock_test".equalsIgnoreCase(providerMode)) {
            String mockWaId = "wamid.HBgL-MOCK-" + UUID.randomUUID().toString().substring(0, 8);
            log.info("Mock WhatsApp message sent to {} (wamid: {})", normalizedPhone, mockWaId);
            return DeliveryResult.builder()
                    .success(true)
                    .status(DeliveryStatus.SENT)
                    .recipientContact(normalizedPhone)
                    .providerReference(mockWaId)
                    .providerResponse("Mock WhatsApp message sent successfully to " + normalizedPhone)
                    .build();
        }

        if ("evolution".equalsIgnoreCase(providerMode)) {
            return sendViaEvolution(normalizedPhone, waPhoneDigits, request);
        }

        try {
            String messageText = composeWhatsAppText(request);

            String jsonPayload = "{" +
                    "\"messaging_product\": \"whatsapp\"," +
                    "\"recipient_type\": \"individual\"," +
                    "\"to\": \"" + waPhoneDigits + "\"," +
                    "\"type\": \"text\"," +
                    "\"text\": {\"preview_url\": true, \"body\": \"" + escapeJson(messageText) + "\"}" +
                    "}";

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(metaMessagesUrl()))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            int statusCode = httpResponse.statusCode();
            String responseBody = httpResponse.body();

            if (statusCode == 200 || statusCode == 201) {
                String messageId = extractMessageIdFromResponse(responseBody);
                String providerRef = messageId != null ? messageId : "wamid.WA-MSG-" + UUID.randomUUID().toString().substring(0, 8);
                log.info("WhatsApp message dispatched successfully via Meta Cloud API to {} (Message ID: {})", normalizedPhone, providerRef);

                return DeliveryResult.builder()
                        .success(true)
                        .status(DeliveryStatus.SENT)
                        .recipientContact(normalizedPhone)
                        .providerReference(providerRef)
                        .providerResponse("WhatsApp message dispatched via Meta API (Status " + statusCode + ")")
                        .build();
            } else {
                log.error("WhatsApp Cloud API HTTP error (HTTP {}): {}", statusCode, responseBody);
                return DeliveryResult.builder()
                        .success(false)
                        .status(DeliveryStatus.FAILED)
                        .recipientContact(normalizedPhone)
                        .errorMessage("WhatsApp Cloud API HTTP error (" + statusCode + "): " + responseBody)
                        .providerResponse(responseBody)
                        .build();
            }
        } catch (Exception e) {
            log.error("WhatsApp Gateway execution exception for {}: {}", normalizedPhone, e.getMessage(), e);
            return DeliveryResult.builder()
                    .success(false)
                    .status(DeliveryStatus.FAILED)
                    .recipientContact(normalizedPhone)
                    .errorMessage("WhatsApp Gateway execution exception: " + e.getMessage())
                    .providerResponse("HTTP Exception: " + e.getClass().getSimpleName())
                    .build();
        }
    }

    static String composeWhatsAppText(DeliveryRequest request) {
        String guestName = request.getGuestName() != null && !request.getGuestName().isBlank()
                ? request.getGuestName().trim()
                : "Guest";
        String eventName = request.getEventName() != null && !request.getEventName().isBlank()
                ? request.getEventName().trim()
                : "Event";
        String invitationUrl = request.getInvitationUrl() != null && !request.getInvitationUrl().isBlank()
                ? request.getInvitationUrl().trim()
                : "";
        String when = formatWhen(request.getEventDate());
        String where = request.getVenue() != null ? request.getVenue().trim() : "";
        if ("Venue".equalsIgnoreCase(where) || "TBD".equalsIgnoreCase(where) || "Venue TBD".equalsIgnoreCase(where)) {
            where = "";
        }

        StringBuilder text = new StringBuilder();
        text.append("You're invited!\n\n");
        text.append("Dear ").append(guestName).append(",\n");
        text.append("You are invited to *").append(eventName).append("*.\n");
        if (!when.isEmpty()) {
            text.append("When: *").append(when).append("*\n");
        }
        if (!where.isEmpty()) {
            text.append("Where: *").append(where).append("*\n");
        }
        if (!invitationUrl.isEmpty()) {
            text.append("\nYour card:\n").append(invitationUrl);
        }
        return text.toString();
    }

    private static String formatWhen(String eventDate) {
        if (eventDate == null || eventDate.isBlank() || "TBD".equalsIgnoreCase(eventDate.trim())) {
            return "";
        }
        String raw = eventDate.trim();
        try {
            java.time.LocalDateTime parsed = java.time.LocalDateTime.parse(
                    raw, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            return parsed.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy, h:mm a", java.util.Locale.ENGLISH));
        } catch (Exception ignored) {
            return raw;
        }
    }

    private String metaMessagesUrl() {
        if (phoneNumberId != null && !phoneNumberId.isBlank()
                && whatsappApiUrl != null && whatsappApiUrl.contains("/me/messages")) {
            return whatsappApiUrl.replace("/me/messages", "/" + phoneNumberId + "/messages");
        }
        return whatsappApiUrl;
    }

    private DeliveryResult sendViaEvolution(String normalizedPhone, String waPhoneDigits, DeliveryRequest request) {
        if (evolutionInstance == null || evolutionInstance.isBlank()
                || evolutionApiKey == null || evolutionApiKey.isBlank()) {
            return DeliveryResult.builder()
                    .success(false)
                    .status(DeliveryStatus.FAILED)
                    .recipientContact(normalizedPhone)
                    .errorMessage("Evolution instance and API key are not configured")
                    .build();
        }

        String messageText = composeWhatsAppText(request);
        byte[] cardPng = request.getCardImageBytes();
        boolean sendImage = cardPng != null && cardPng.length > 0;

        try {
            String base = evolutionBaseUrl == null || evolutionBaseUrl.isBlank()
                    ? "http://127.0.0.1:8081"
                    : evolutionBaseUrl.replaceAll("/+$", "");
            String instance = java.net.URLEncoder.encode(evolutionInstance, StandardCharsets.UTF_8)
                    .replace("+", "%20");
            String endpoint = sendImage
                    ? base + "/message/sendMedia/" + instance
                    : base + "/message/sendText/" + instance;

            String jsonPayload;
            if (sendImage) {
                String fileName = request.getCardImageFileName() != null && !request.getCardImageFileName().isBlank()
                        ? request.getCardImageFileName()
                        : "invitation-card.png";
                jsonPayload = "{\"number\":\"" + escapeJson(waPhoneDigits) + "\","
                        + "\"mediatype\":\"image\","
                        + "\"mimetype\":\"image/png\","
                        + "\"fileName\":\"" + escapeJson(fileName) + "\","
                        + "\"caption\":\"" + escapeJson(messageText) + "\","
                        + "\"media\":\"" + Base64.getEncoder().encodeToString(cardPng) + "\"}";
            } else {
                jsonPayload = "{\"number\":\"" + escapeJson(waPhoneDigits) + "\",\"text\":\""
                        + escapeJson(messageText) + "\"}";
            }

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("apikey", evolutionApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(45))
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            int statusCode = httpResponse.statusCode();
            String responseBody = httpResponse.body();

            if (statusCode == 200 || statusCode == 201) {
                String messageId = extractNestedKeyId(responseBody);
                String providerRef = messageId != null ? messageId : "EVO-" + UUID.randomUUID().toString().substring(0, 8);
                log.info("WhatsApp message accepted by Evolution for {} (id: {}, image: {})",
                        normalizedPhone, providerRef, sendImage);
                return DeliveryResult.builder()
                        .success(true)
                        .status(DeliveryStatus.SENT)
                        .recipientContact(normalizedPhone)
                        .providerReference(providerRef)
                        .providerResponse("Evolution accepted message (HTTP " + statusCode + ")")
                        .build();
            }

            log.error("Evolution API HTTP error (HTTP {}): {}", statusCode, responseBody);
            return DeliveryResult.builder()
                    .success(false)
                    .status(DeliveryStatus.FAILED)
                    .recipientContact(normalizedPhone)
                    .errorMessage("Evolution API HTTP error (" + statusCode + "): " + responseBody)
                    .providerResponse(responseBody)
                    .build();
        } catch (Exception e) {
            log.error("Evolution API exception for {}: {}", normalizedPhone, e.getMessage(), e);
            return DeliveryResult.builder()
                    .success(false)
                    .status(DeliveryStatus.FAILED)
                    .recipientContact(normalizedPhone)
                    .errorMessage("WhatsApp Gateway execution exception: " + e.getMessage())
                    .providerResponse("HTTP Exception: " + e.getClass().getSimpleName())
                    .build();
        }
    }

    private String extractNestedKeyId(String body) {
        if (body == null) {
            return null;
        }
        int keyIdx = body.indexOf("\"key\"");
        String region = keyIdx >= 0 ? body.substring(keyIdx) : body;
        return extractMessageIdFromResponse(region);
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String extractMessageIdFromResponse(String body) {
        if (body != null && body.contains("\"id\":")) {
            int idx = body.indexOf("\"id\":");
            int start = body.indexOf("\"", idx + 5) + 1;
            int end = body.indexOf("\"", start);
            if (start > 0 && end > start) {
                return body.substring(start, end);
            }
        }
        return null;
    }
}
