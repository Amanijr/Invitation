package com.InvitationSystem.InvitationSystem.service;

import com.InvitationSystem.InvitationSystem.Dto.deliveryDto.DeliveryRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.deliveryDto.MultiChannelDeliveryResponseDto;
import com.InvitationSystem.InvitationSystem.entity.*;
import com.InvitationSystem.InvitationSystem.provider.DeliveryRequest;
import com.InvitationSystem.InvitationSystem.provider.DeliveryResult;
import com.InvitationSystem.InvitationSystem.provider.impl.SmsChannelProvider;
import com.InvitationSystem.InvitationSystem.repository.DeliveryLogRepository;
import com.InvitationSystem.InvitationSystem.repository.EventRepository;
import com.InvitationSystem.InvitationSystem.repository.GuestRepository;
import com.InvitationSystem.InvitationSystem.repository.InvitationRepository;
import com.InvitationSystem.InvitationSystem.service.impl.MultiChannelDeliveryServiceImpl;
import com.InvitationSystem.InvitationSystem.util.PDFService;
import com.InvitationSystem.InvitationSystem.util.TemplateProcessorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmsDeliveryPhase9Test {

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private GuestRepository guestRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private DeliveryLogRepository deliveryLogRepository;

    @Mock
    private ECardRenderingEngineService eCardRenderingEngineService;

    @Mock
    private TemplateProcessorService templateProcessorService;

    @Mock
    private PDFService pdfService;

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<String> mockHttpResponse;

    @Captor
    private ArgumentCaptor<DeliveryLog> deliveryLogCaptor;

    private SmsChannelProvider smsChannelProvider;
    private MultiChannelDeliveryServiceImpl deliveryService;

    private UUID invitationId;
    private UUID guestId;
    private UUID eventId;
    private Invitation validInvitation;
    private Guest validGuest;
    private Event validEvent;

    @BeforeEach
    void setUp() {
        invitationId = UUID.randomUUID();
        guestId = UUID.randomUUID();
        eventId = UUID.randomUUID();

        smsChannelProvider = new SmsChannelProvider();
        smsChannelProvider.setHttpClient(mockHttpClient);
        ReflectionTestUtils.setField(smsChannelProvider, "providerMode", "twilio");
        ReflectionTestUtils.setField(smsChannelProvider, "accountSid", "AC_test_account_sid");
        ReflectionTestUtils.setField(smsChannelProvider, "authToken", "test_auth_token");
        ReflectionTestUtils.setField(smsChannelProvider, "fromNumber", "+15005550006");
        ReflectionTestUtils.setField(smsChannelProvider, "smsApiUrl", "https://api.twilio.com/2010-04-01/Accounts");

        deliveryService = new MultiChannelDeliveryServiceImpl(List.of(smsChannelProvider));
        ReflectionTestUtils.setField(deliveryService, "invitationRepository", invitationRepository);
        ReflectionTestUtils.setField(deliveryService, "guestRepository", guestRepository);
        ReflectionTestUtils.setField(deliveryService, "eventRepository", eventRepository);
        ReflectionTestUtils.setField(deliveryService, "deliveryLogRepository", deliveryLogRepository);
        ReflectionTestUtils.setField(deliveryService, "eCardRenderingEngineService", eCardRenderingEngineService);
        ReflectionTestUtils.setField(deliveryService, "templateProcessorService", templateProcessorService);
        ReflectionTestUtils.setField(deliveryService, "pdfService", pdfService);
        ReflectionTestUtils.setField(deliveryService, "publicUrl", "http://localhost:5173");

        validGuest = Guest.builder()
                .id(guestId)
                .eventId(eventId)
                .fullName("Neema Joseph")
                .phone("0712345678")
                .email("neema@example.com")
                .build();

        validEvent = Event.builder()
                .id(eventId)
                .eventName("Amani & Neema Gala")
                .eventDate(LocalDateTime.now().plusDays(7))
                .venue("Kilimanjaro Hotel")
                .build();

        validInvitation = Invitation.builder()
                .id(invitationId)
                .eventId(eventId)
                .guestId(guestId)
                .recipientPhone("0712345678")
                .uniqueToken("token-neema-777")
                .status(InvitationStatus.GENERATED)
                .deliveryStatus(DeliveryStatus.PENDING)
                .build();

        lenient().when(deliveryLogRepository.save(any(DeliveryLog.class))).thenAnswer(inv -> {
            DeliveryLog log = inv.getArgument(0);
            if (log.getId() == null) {
                log.setId(UUID.randomUUID());
            }
            return log;
        });
    }

    @Test
    @DisplayName("1. Valid phone number — normalizes to E.164 and dispatches SMS successfully")
    void test1_ValidPhoneNumberDelivery() throws Exception {
        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(validInvitation));
        when(guestRepository.findById(guestId)).thenReturn(Optional.of(validGuest));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(validEvent));

        when(mockHttpResponse.statusCode()).thenReturn(201);
        when(mockHttpResponse.body()).thenReturn("{\"sid\": \"SM999888777666555444333222111000\"}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);

        DeliveryRequestDto request = DeliveryRequestDto.builder()
                .invitationId(invitationId)
                .channels(List.of(DeliveryChannel.SMS))
                .build();

        MultiChannelDeliveryResponseDto response = deliveryService.sendInvitation(request);

        assertNotNull(response);
        assertEquals(DeliveryStatus.SENT, response.getOverallStatus());
        assertEquals(1, response.getResults().size());
        assertEquals("+255712345678", response.getResults().get(0).getRecipientContact());
        assertEquals("SM999888777666555444333222111000", response.getResults().get(0).getProviderReference());
    }

    @Test
    @DisplayName("2. Invalid phone number format — rejected as FAILED")
    void test2_InvalidPhoneNumberFormat() {
        validInvitation.setRecipientPhone("abc-not-a-phone");

        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(validInvitation));
        when(guestRepository.findById(guestId)).thenReturn(Optional.of(validGuest));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(validEvent));

        DeliveryRequestDto request = DeliveryRequestDto.builder()
                .invitationId(invitationId)
                .channels(List.of(DeliveryChannel.SMS))
                .recipientPhone("abc-not-a-phone")
                .build();

        MultiChannelDeliveryResponseDto response = deliveryService.sendInvitation(request);

        assertNotNull(response);
        assertEquals(DeliveryStatus.FAILED, response.getOverallStatus());
        assertTrue(response.getResults().get(0).getErrorMessage().contains("Invalid phone number format"));
        verifyNoInteractions(mockHttpClient);
    }

    @Test
    @DisplayName("3. Missing phone number — rejected as FAILED")
    void test3_MissingPhoneNumberRejection() {
        validGuest.setPhone(null);
        validInvitation.setRecipientPhone(null);

        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(validInvitation));
        when(guestRepository.findById(guestId)).thenReturn(Optional.of(validGuest));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(validEvent));

        DeliveryRequestDto request = DeliveryRequestDto.builder()
                .invitationId(invitationId)
                .channels(List.of(DeliveryChannel.SMS))
                .build();

        MultiChannelDeliveryResponseDto response = deliveryService.sendInvitation(request);

        assertNotNull(response);
        assertEquals(DeliveryStatus.FAILED, response.getOverallStatus());
        assertTrue(response.getResults().get(0).getErrorMessage().contains("missing or empty"));
        verifyNoInteractions(mockHttpClient);
    }

    @Test
    @DisplayName("4. Provider HTTP success — extracts Twilio SID from 200/201 response")
    void test4_ProviderHttpSuccess() throws Exception {
        DeliveryRequest req = DeliveryRequest.builder()
                .invitationId(invitationId)
                .guestName("Neema Joseph")
                .eventName("Gala")
                .recipientPhone("+255712345678")
                .invitationUrl("http://localhost:8080/api/v1/invitations/token/token-123")
                .channel(DeliveryChannel.SMS)
                .build();

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("{\"sid\": \"SMabcdef1234567890\", \"status\": \"queued\"}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);

        DeliveryResult result = smsChannelProvider.send(req);

        assertTrue(result.isSuccess());
        assertEquals(DeliveryStatus.SENT, result.getStatus());
        assertEquals("SMabcdef1234567890", result.getProviderReference());
    }

    @Test
    @DisplayName("5. Provider HTTP error — 400 Bad Request recorded as FAILED")
    void test5_ProviderHttpError() throws Exception {
        DeliveryRequest req = DeliveryRequest.builder()
                .invitationId(invitationId)
                .guestName("Neema Joseph")
                .eventName("Gala")
                .recipientPhone("+255712345678")
                .channel(DeliveryChannel.SMS)
                .build();

        when(mockHttpResponse.statusCode()).thenReturn(400);
        when(mockHttpResponse.body()).thenReturn("{\"code\": 21211, \"message\": \"Invalid phone number\"}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);

        DeliveryResult result = smsChannelProvider.send(req);

        assertFalse(result.isSuccess());
        assertEquals(DeliveryStatus.FAILED, result.getStatus());
        assertTrue(result.getErrorMessage().contains("HTTP error (400)"));
    }

    @Test
    @DisplayName("6. Provider connection exception — caught safely as FAILED")
    void test6_ProviderExceptionHandling() throws Exception {
        DeliveryRequest req = DeliveryRequest.builder()
                .invitationId(invitationId)
                .guestName("Neema Joseph")
                .eventName("Gala")
                .recipientPhone("+255712345678")
                .channel(DeliveryChannel.SMS)
                .build();

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("Gateway connection timeout"));

        DeliveryResult result = smsChannelProvider.send(req);

        assertFalse(result.isSuccess());
        assertEquals(DeliveryStatus.FAILED, result.getStatus());
        assertTrue(result.getErrorMessage().contains("Gateway connection timeout"));
    }

    @Test
    @DisplayName("7. Duplicate send — idempotency policy prevents duplicate SMS dispatch")
    void test7_DuplicateSendProtection() {
        String key = "DELIVERY:" + invitationId + ":SMS";
        DeliveryLog existingSentLog = DeliveryLog.builder()
                .id(UUID.randomUUID())
                .invitationId(invitationId)
                .channel("SMS")
                .status("SENT")
                .idempotencyKey(key)
                .providerReference("SM-PREVIOUS-SID-111")
                .sentAt(LocalDateTime.now().minusMinutes(5))
                .build();

        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(validInvitation));
        when(guestRepository.findById(guestId)).thenReturn(Optional.of(validGuest));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(validEvent));
        when(deliveryLogRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(existingSentLog));

        DeliveryRequestDto request = DeliveryRequestDto.builder()
                .invitationId(invitationId)
                .channels(List.of(DeliveryChannel.SMS))
                .build();

        MultiChannelDeliveryResponseDto response = deliveryService.sendInvitation(request);

        assertNotNull(response);
        assertEquals(DeliveryStatus.SENT, response.getOverallStatus());
        assertEquals("SM-PREVIOUS-SID-111", response.getResults().get(0).getProviderReference());
        verifyNoInteractions(mockHttpClient);
    }

    @Test
    @DisplayName("8. Unauthorized sending / Cross-event guest rejection")
    void test8_CrossEventRejection() {
        UUID otherEventId = UUID.randomUUID();
        Guest mismatchedGuest = Guest.builder()
                .id(guestId)
                .eventId(otherEventId)
                .fullName("Foreign Guest")
                .phone("0712345678")
                .build();

        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(validInvitation));
        when(guestRepository.findById(guestId)).thenReturn(Optional.of(mismatchedGuest));

        DeliveryRequestDto request = DeliveryRequestDto.builder()
                .invitationId(invitationId)
                .channels(List.of(DeliveryChannel.SMS))
                .build();

        assertThrows(IllegalArgumentException.class, () -> deliveryService.sendInvitation(request));
        verifyNoInteractions(mockHttpClient);
    }

    @Test
    @DisplayName("9. Invalid invitation ID rejection")
    void test9_InvalidInvitationID() {
        UUID invalidId = UUID.randomUUID();
        when(invitationRepository.findById(invalidId)).thenReturn(Optional.empty());

        DeliveryRequestDto request = DeliveryRequestDto.builder()
                .invitationId(invalidId)
                .channels(List.of(DeliveryChannel.SMS))
                .build();

        assertThrows(IllegalArgumentException.class, () -> deliveryService.sendInvitation(request));
    }

    @Test
    @DisplayName("10. Delivery audit record validation")
    void test10_DeliveryAuditRecordValidation() throws Exception {
        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(validInvitation));
        when(guestRepository.findById(guestId)).thenReturn(Optional.of(validGuest));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(validEvent));

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("{\"sid\": \"SM-AUDIT-SID-888\"}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);

        DeliveryRequestDto request = DeliveryRequestDto.builder()
                .invitationId(invitationId)
                .channels(List.of(DeliveryChannel.SMS))
                .build();

        deliveryService.sendInvitation(request);

        verify(deliveryLogRepository, atLeastOnce()).save(deliveryLogCaptor.capture());
        List<DeliveryLog> savedLogs = deliveryLogCaptor.getAllValues();
        DeliveryLog lastLog = savedLogs.get(savedLogs.size() - 1);

        assertEquals(invitationId, lastLog.getInvitationId());
        assertEquals(guestId, lastLog.getGuestId());
        assertEquals("SMS", lastLog.getChannel());
        assertEquals("SENT", lastLog.getStatus());
        assertEquals("+255712345678", lastLog.getRecipientContact());
        assertEquals("SM-AUDIT-SID-888", lastLog.getProviderReference());
        assertNotNull(lastLog.getSentAt());
    }

    @Test
    @DisplayName("11. Android SMS Gateway — HTTP 202 is SENT, never DELIVERED")
    void test11_AndroidSmsGateHttpAcceptedIsSent() throws Exception {
        ReflectionTestUtils.setField(smsChannelProvider, "providerMode", "android_smsgate");
        ReflectionTestUtils.setField(smsChannelProvider, "androidBaseUrl", "https://api.sms-gate.app");
        ReflectionTestUtils.setField(smsChannelProvider, "androidUsername", "desk");
        ReflectionTestUtils.setField(smsChannelProvider, "androidPassword", "secret");

        DeliveryRequest req = DeliveryRequest.builder()
                .invitationId(invitationId)
                .guestName("Neema Joseph")
                .eventName("Gala")
                .recipientPhone("+255712345678")
                .channel(DeliveryChannel.SMS)
                .build();

        when(mockHttpResponse.statusCode()).thenReturn(202);
        when(mockHttpResponse.body()).thenReturn("{\"id\":\"zXDYfTmTVf3iMd16zzdBj\",\"state\":\"Pending\"}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);

        DeliveryResult result = smsChannelProvider.send(req);

        ArgumentCaptor<HttpRequest> httpCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient).send(httpCaptor.capture(), any(HttpResponse.BodyHandler.class));
        HttpRequest sent = httpCaptor.getValue();
        assertEquals("https://api.sms-gate.app/3rdparty/v1/messages", sent.uri().toString());
        assertTrue(sent.headers().firstValue("Authorization").orElse("").startsWith("Basic "));
        assertEquals("application/json", sent.headers().firstValue("Content-Type").orElse(""));

        assertTrue(result.isSuccess());
        assertEquals(DeliveryStatus.SENT, result.getStatus());
        assertNotEquals(DeliveryStatus.DELIVERED, result.getStatus());
        assertEquals("zXDYfTmTVf3iMd16zzdBj", result.getProviderReference());
    }

    @Test
    @DisplayName("12. Android SMS Gateway — HTTP 401 is FAILED")
    void test12_AndroidSmsGateUnauthorized() throws Exception {
        ReflectionTestUtils.setField(smsChannelProvider, "providerMode", "android_smsgate");
        ReflectionTestUtils.setField(smsChannelProvider, "androidUsername", "desk");
        ReflectionTestUtils.setField(smsChannelProvider, "androidPassword", "secret");

        DeliveryRequest req = DeliveryRequest.builder()
                .invitationId(invitationId)
                .recipientPhone("+255712345678")
                .channel(DeliveryChannel.SMS)
                .build();

        when(mockHttpResponse.statusCode()).thenReturn(401);
        when(mockHttpResponse.body()).thenReturn("{\"message\":\"Unauthorized\"}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);

        DeliveryResult result = smsChannelProvider.send(req);

        assertFalse(result.isSuccess());
        assertEquals(DeliveryStatus.FAILED, result.getStatus());
        assertTrue(result.getErrorMessage().contains("HTTP error (401)"));
    }

    @Test
    @DisplayName("13. Android SMS Gateway — missing credentials fail without HTTP")
    void test13_AndroidSmsGateMissingCredentials() {
        ReflectionTestUtils.setField(smsChannelProvider, "providerMode", "android_smsgate");
        ReflectionTestUtils.setField(smsChannelProvider, "androidUsername", "");
        ReflectionTestUtils.setField(smsChannelProvider, "androidPassword", "");

        DeliveryRequest req = DeliveryRequest.builder()
                .invitationId(invitationId)
                .recipientPhone("+255712345678")
                .channel(DeliveryChannel.SMS)
                .build();

        DeliveryResult result = smsChannelProvider.send(req);

        assertFalse(result.isSuccess());
        assertEquals(DeliveryStatus.FAILED, result.getStatus());
        assertTrue(result.getErrorMessage().contains("username and password"));
        verifyNoInteractions(mockHttpClient);
    }
}
