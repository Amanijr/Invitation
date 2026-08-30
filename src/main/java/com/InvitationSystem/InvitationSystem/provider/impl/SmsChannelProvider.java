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
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

@Component
public class SmsChannelProvider implements ChannelProvider {

    private static final Logger log = LoggerFactory.getLogger(SmsChannelProvider.class);

    @Value("${delivery.sms.provider:twilio}")
    private String providerMode;

    @Value("${delivery.sms.twilio.account-sid:${SMS_ACCOUNT_SID:${TWILIO_ACCOUNT_SID:AC_test_account_sid_placeholder}}}")
    private String accountSid;

    @Value("${delivery.sms.twilio.auth-token:${SMS_AUTH_TOKEN:${TWILIO_AUTH_TOKEN:test_auth_token_placeholder}}}")
    private String authToken;

    @Value("${delivery.sms.twilio.from-number:${SMS_FROM_NUMBER:${TWILIO_FROM_NUMBER:+15005550006}}}")
    private String fromNumber;

    @Value("${delivery.sms.api-url:${SMS_API_URL:https://api.twilio.com/2010-04-01/Accounts}}")
    private String smsApiUrl;

    @Value("${delivery.sms.android.base-url:https://api.sms-gate.app}")
    private String androidBaseUrl;

    @Value("${delivery.sms.android.username:}")
    private String androidUsername;

    @Value("${delivery.sms.android.password:}")
    private String androidPassword;

    @Value("${delivery.sms.android.device-id:}")
    private String androidDeviceId;

    private HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public DeliveryChannel getChannel() {
        return DeliveryChannel.SMS;
    }

    @Override
    public DeliveryResult send(DeliveryRequest request) {
        String rawPhone = request.getRecipientPhone();

        if (rawPhone == null || rawPhone.isBlank()) {
            log.warn("SMS delivery rejected: Recipient phone number is missing or empty for invitation ID: {}", request.getInvitationId());
            return DeliveryResult.builder()
                    .success(false)
                    .status(DeliveryStatus.FAILED)
                    .recipientContact(rawPhone)
                    .errorMessage("Recipient phone number is missing or empty")
                    .build();
        }

        String normalizedPhone = PhoneNormalizationUtil.normalizePhoneNumber(rawPhone);
        if (!PhoneNormalizationUtil.isValidE164(normalizedPhone)) {
            log.warn("SMS delivery rejected: Invalid E.164 phone format '{}' (raw: '{}') for invitation ID: {}", normalizedPhone, rawPhone, request.getInvitationId());
            return DeliveryResult.builder()
                    .success(false)
                    .status(DeliveryStatus.FAILED)
                    .recipientContact(rawPhone)
                    .errorMessage("Invalid phone number format for SMS: " + rawPhone)
                    .build();
        }

        String smsText = composeSmsText(request);

        if ("mock_test".equalsIgnoreCase(providerMode)) {
            String mockMessageSid = "SMS-MOCK-" + UUID.randomUUID().toString().substring(0, 8);
            log.info("Mock SMS sent to {} (Sid: {})", normalizedPhone, mockMessageSid);
            return DeliveryResult.builder()
                    .success(true)
                    .status(DeliveryStatus.SENT)
                    .recipientContact(normalizedPhone)
                    .providerReference(mockMessageSid)
                    .providerResponse("Mock SMS sent successfully to " + normalizedPhone)
                    .build();
        }

        if ("android_smsgate".equalsIgnoreCase(providerMode)) {
            return sendViaAndroidSmsGate(normalizedPhone, smsText);
        }

        try {
            String endpoint = smsApiUrl.endsWith("/")
                    ? smsApiUrl + accountSid + "/Messages.json"
                    : smsApiUrl + "/" + accountSid + "/Messages.json";

            String formData = "To=" + URLEncoder.encode(normalizedPhone, StandardCharsets.UTF_8)
                    + "&From=" + URLEncoder.encode(fromNumber, StandardCharsets.UTF_8)
                    + "&Body=" + URLEncoder.encode(smsText, StandardCharsets.UTF_8);

            String authHeader = "Basic " + Base64.getEncoder()
                    .encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Authorization", authHeader)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formData))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            int statusCode = httpResponse.statusCode();
            String responseBody = httpResponse.body();

            if (statusCode == 200 || statusCode == 201) {
                String sid = extractSidFromResponse(responseBody);
                String providerRef = sid != null ? sid : "SMS-SID-" + UUID.randomUUID().toString().substring(0, 8);
                log.info("SMS dispatched successfully via Gateway to {} (Provider Ref: {})", normalizedPhone, providerRef);
                return DeliveryResult.builder()
                        .success(true)
                        .status(DeliveryStatus.SENT)
                        .recipientContact(normalizedPhone)
                        .providerReference(providerRef)
                        .providerResponse("SMS dispatched successfully via Twilio Gateway (Status " + statusCode + ")")
                        .build();
            } else {
                log.error("SMS Gateway API error (HTTP {}): {}", statusCode, responseBody);
                return DeliveryResult.builder()
                        .success(false)
                        .status(DeliveryStatus.FAILED)
                        .recipientContact(normalizedPhone)
                        .errorMessage("SMS Provider API HTTP error (" + statusCode + "): " + responseBody)
                        .providerResponse(responseBody)
                        .build();
            }
        } catch (Exception e) {
            log.error("SMS Gateway execution exception for {}: {}", normalizedPhone, e.getMessage(), e);
            return DeliveryResult.builder()
                    .success(false)
                    .status(DeliveryStatus.FAILED)
                    .recipientContact(normalizedPhone)
                    .errorMessage("SMS Gateway execution exception: " + e.getMessage())
                    .providerResponse("HTTP Exception: " + e.getClass().getSimpleName())
                    .build();
        }
    }

    static String composeSmsText(DeliveryRequest request) {
        String guestName = request.getGuestName() != null && !request.getGuestName().isBlank()
                ? request.getGuestName()
                : "Guest";
        String eventName = request.getEventName() != null && !request.getEventName().isBlank()
                ? request.getEventName()
                : "Event";
        String invitationUrl = request.getInvitationUrl() != null && !request.getInvitationUrl().isBlank()
                ? request.getInvitationUrl()
                : "";
        String doorCode = request.getInvitationToken() != null ? request.getInvitationToken().trim() : "";

        StringBuilder text = new StringBuilder();
        text.append("Hi ").append(guestName).append(", you're invited to ").append(eventName).append(".");
        if (!doorCode.isEmpty()) {
            text.append(" Door code: ").append(doorCode).append(".");
        }
        if (!invitationUrl.isEmpty()) {
            text.append(" Card: ").append(invitationUrl);
        }
        return text.toString();
    }

    private DeliveryResult sendViaAndroidSmsGate(String normalizedPhone, String smsText) {
        if (androidUsername == null || androidUsername.isBlank()
                || androidPassword == null || androidPassword.isBlank()) {
            return DeliveryResult.builder()
                    .success(false)
                    .status(DeliveryStatus.FAILED)
                    .recipientContact(normalizedPhone)
                    .errorMessage("Android SMS Gateway username and password are not configured")
                    .build();
        }

        try {
            String base = androidBaseUrl == null || androidBaseUrl.isBlank()
                    ? "https://api.sms-gate.app"
                    : androidBaseUrl.replaceAll("/+$", "");
            String endpoint = base + "/3rdparty/v1/messages";

            StringBuilder json = new StringBuilder();
            json.append("{\"phoneNumbers\":[\"").append(escapeJson(normalizedPhone)).append("\"],");
            json.append("\"textMessage\":{\"text\":\"").append(escapeJson(smsText)).append("\"}");
            if (androidDeviceId != null && !androidDeviceId.isBlank()) {
                json.append(",\"deviceId\":\"").append(escapeJson(androidDeviceId)).append("\"");
            }
            json.append("}");

            String authHeader = "Basic " + Base64.getEncoder()
                    .encodeToString((androidUsername + ":" + androidPassword).getBytes(StandardCharsets.UTF_8));

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Authorization", authHeader)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString(), StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            int statusCode = httpResponse.statusCode();
            String responseBody = httpResponse.body();

            if (statusCode == 200 || statusCode == 201 || statusCode == 202) {
                String messageId = extractJsonField(responseBody, "id");
                String providerRef = messageId != null ? messageId : "SMSGATE-" + UUID.randomUUID().toString().substring(0, 8);
                log.info("SMS accepted by Android SMS Gateway for {} (id: {})", normalizedPhone, providerRef);
                return DeliveryResult.builder()
                        .success(true)
                        .status(DeliveryStatus.SENT)
                        .recipientContact(normalizedPhone)
                        .providerReference(providerRef)
                        .providerResponse("Android SMS Gateway accepted message (HTTP " + statusCode + ")")
                        .build();
            }

            log.error("Android SMS Gateway error (HTTP {}): {}", statusCode, responseBody);
            return DeliveryResult.builder()
                    .success(false)
                    .status(DeliveryStatus.FAILED)
                    .recipientContact(normalizedPhone)
                    .errorMessage("SMS Provider API HTTP error (" + statusCode + "): " + responseBody)
                    .providerResponse(responseBody)
                    .build();
        } catch (Exception e) {
            log.error("Android SMS Gateway exception for {}: {}", normalizedPhone, e.getMessage(), e);
            return DeliveryResult.builder()
                    .success(false)
                    .status(DeliveryStatus.FAILED)
                    .recipientContact(normalizedPhone)
                    .errorMessage("SMS Gateway execution exception: " + e.getMessage())
                    .providerResponse("HTTP Exception: " + e.getClass().getSimpleName())
                    .build();
        }
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private String extractJsonField(String body, String field) {
        if (body == null) {
            return null;
        }
        String needle = "\"" + field + "\":";
        int idx = body.indexOf(needle);
        if (idx < 0) {
            return null;
        }
        int start = body.indexOf("\"", idx + needle.length());
        if (start < 0) {
            return null;
        }
        int end = body.indexOf("\"", start + 1);
        if (end <= start) {
            return null;
        }
        return body.substring(start + 1, end);
    }

    private String extractSidFromResponse(String body) {
        if (body != null && body.contains("\"sid\":")) {
            int idx = body.indexOf("\"sid\":");
            int start = body.indexOf("\"", idx + 6) + 1;
            int end = body.indexOf("\"", start);
            if (start > 0 && end > start) {
                return body.substring(start, end);
            }
        }
        return null;
    }
}
