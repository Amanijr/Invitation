package com.InvitationSystem.InvitationSystem.service;

import com.InvitationSystem.InvitationSystem.Dto.deliveryDto.DeliveryRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.deliveryDto.MultiChannelDeliveryResponseDto;
import com.InvitationSystem.InvitationSystem.Dto.deliveryLogsDto.DeliveryLogResponseDto;
import com.InvitationSystem.InvitationSystem.entity.*;
import com.InvitationSystem.InvitationSystem.provider.DeliveryRequest;
import com.InvitationSystem.InvitationSystem.provider.DeliveryResult;
import com.InvitationSystem.InvitationSystem.provider.impl.WhatsAppChannelProvider;
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
class WhatsAppDeliveryPhase10Test {

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

    private WhatsAppChannelProvider whatsAppChannelProvider;
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

        whatsAppChannelProvider = new WhatsAppChannelProvider();
        whatsAppChannelProvider.setHttpClient(mockHttpClient);
        ReflectionTestUtils.setField(whatsAppChannelProvider, "providerMode", "meta");
        ReflectionTestUtils.setField(whatsAppChannelProvider, "whatsappApiUrl", "https://graph.facebook.com/v18.0/100000000000000/messages");
        ReflectionTestUtils.setField(whatsAppChannelProvider, "accessToken", "EAAG_test_meta_token_123");
        ReflectionTestUtils.setField(whatsAppChannelProvider, "fromNumber", "+15005550006");

        deliveryService = new MultiChannelDeliveryServiceImpl(List.of(whatsAppChannelProvider));
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
                .fullName("Amani Neema")
                .phone("0712999888")
                .email("amani@example.com")
                .build();

        validEvent = Event.builder()
                .id(eventId)
                .eventName("Amani & Neema Wedding")
                .eventDate(LocalDateTime.now().plusDays(30))
                .venue("Dar es Salaam Serena Hotel")
                .build();

        validInvitation = Invitation.builder()
                .id(invitationId)
                .eventId(eventId)
                .guestId(guestId)
                .recipientPhone("0712999888")
                .uniqueToken("token-wa-amani-100")
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
    @DisplayName("1. Valid WhatsApp recipient — E.164 normalization and dispatch via Meta Cloud API")
    void test1_ValidRecipientDelivery() throws Exception {
        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(validInvitation));
        when(guestRepository.findById(guestId)).thenReturn(Optional.of(validGuest));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(validEvent));

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("{\"messaging_product\":\"whatsapp\",\"messages\":[{\"id\":\"wamid.HBgLMjU1NzEyOTk5ODg4FQIA\"}]}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);

        DeliveryRequestDto request = DeliveryRequestDto.builder()
                .invitationId(invitationId)
                .channels(List.of(DeliveryChannel.WHATSAPP))
                .build();

        MultiChannelDeliveryResponseDto response = deliveryService.sendInvitation(request);

        assertNotNull(response);
        assertEquals(DeliveryStatus.SENT, response.getOverallStatus());
        assertEquals(1, response.getResults().size());
        assertEquals("+255712999888", response.getResults().get(0).getRecipientContact());
        assertEquals("wamid.HBgLMjU1NzEyOTk5ODg4FQIA", response.getResults().get(0).getProviderReference());
    }

    @Test
    @DisplayName("2. Invalid recipient phone format — rejected as FAILED")
    void test2_InvalidRecipientPhoneFormat() {
        validInvitation.setRecipientPhone("invalid-phone-str");

        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(validInvitation));
        when(guestRepository.findById(guestId)).thenReturn(Optional.of(validGuest));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(validEvent));

        DeliveryRequestDto request = DeliveryRequestDto.builder()
                .invitationId(invitationId)
                .channels(List.of(DeliveryChannel.WHATSAPP))
                .recipientPhone("invalid-phone-str")
                .build();

        MultiChannelDeliveryResponseDto response = deliveryService.sendInvitation(request);

        assertNotNull(response);
        assertEquals(DeliveryStatus.FAILED, response.getOverallStatus());
        assertTrue(response.getResults().get(0).getErrorMessage().contains("Invalid recipient phone number format"));
        verifyNoInteractions(mockHttpClient);
    }

    @Test
    @DisplayName("3. Missing recipient phone number — rejected as FAILED")
    void test3_MissingRecipientPhoneNumber() {
        validGuest.setPhone(null);
        validInvitation.setRecipientPhone(null);

        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(validInvitation));
        when(guestRepository.findById(guestId)).thenReturn(Optional.of(validGuest));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(validEvent));

        DeliveryRequestDto request = DeliveryRequestDto.builder()
                .invitationId(invitationId)
                .channels(List.of(DeliveryChannel.WHATSAPP))
                .build();

        MultiChannelDeliveryResponseDto response = deliveryService.sendInvitation(request);

        assertNotNull(response);
        assertEquals(DeliveryStatus.FAILED, response.getOverallStatus());
        assertTrue(response.getResults().get(0).getErrorMessage().contains("missing or empty"));
        verifyNoInteractions(mockHttpClient);
    }

    @Test
    @DisplayName("4. Provider HTTP success — extracts Meta wamid from 200/201 response")
    void test4_ProviderHttpSuccess() throws Exception {
        DeliveryRequest req = DeliveryRequest.builder()
                .invitationId(invitationId)
                .guestName("Amani Neema")
                .eventName("Wedding")
                .recipientPhone("+255712999888")
                .invitationUrl("http://localhost:8080/api/v1/invitations/token/token-100")
                .channel(DeliveryChannel.WHATSAPP)
                .build();

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("{\"messaging_product\":\"whatsapp\",\"messages\":[{\"id\":\"wamid.HBgLTEST999\"}]}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);

        DeliveryResult result = whatsAppChannelProvider.send(req);

        assertTrue(result.isSuccess());
        assertEquals(DeliveryStatus.SENT, result.getStatus());
        assertEquals("wamid.HBgLTEST999", result.getProviderReference());
    }

    @Test
    @DisplayName("5. Provider HTTP error — 400 Meta OAuthException recorded as FAILED")
    void test5_ProviderHttpError() throws Exception {
        DeliveryRequest req = DeliveryRequest.builder()
                .invitationId(invitationId)
                .guestName("Amani Neema")
                .eventName("Wedding")
                .recipientPhone("+255712999888")
                .channel(DeliveryChannel.WHATSAPP)
                .build();

        when(mockHttpResponse.statusCode()).thenReturn(400);
        when(mockHttpResponse.body()).thenReturn("{\"error\":{\"message\":\"Invalid OAuth access token\",\"type\":\"OAuthException\",\"code\":190}}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);

        DeliveryResult result = whatsAppChannelProvider.send(req);

        assertFalse(result.isSuccess());
        assertEquals(DeliveryStatus.FAILED, result.getStatus());
        assertTrue(result.getErrorMessage().contains("HTTP error (400)"));
    }

    @Test
    @DisplayName("6. Network failure / connection exception — caught safely as FAILED")
    void test6_NetworkFailureException() throws Exception {
        DeliveryRequest req = DeliveryRequest.builder()
                .invitationId(invitationId)
                .guestName("Amani Neema")
                .eventName("Wedding")
                .recipientPhone("+255712999888")
                .channel(DeliveryChannel.WHATSAPP)
                .build();

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("Meta API Network Connection Refused"));

        DeliveryResult result = whatsAppChannelProvider.send(req);

        assertFalse(result.isSuccess());
        assertEquals(DeliveryStatus.FAILED, result.getStatus());
        assertTrue(result.getErrorMessage().contains("Meta API Network Connection Refused"));
    }

    @Test
    @DisplayName("7. Duplicate prevention — idempotency check bypasses repeat WhatsApp dispatch")
    void test7_DuplicatePreventionIdempotency() {
        String key = "DELIVERY:" + invitationId + ":WHATSAPP";
        DeliveryLog existingSentLog = DeliveryLog.builder()
                .id(UUID.randomUUID())
                .invitationId(invitationId)
                .channel("WHATSAPP")
                .status("SENT")
                .idempotencyKey(key)
                .providerReference("wamid.HBgL-EXISTING-WA-999")
                .sentAt(LocalDateTime.now().minusMinutes(20))
                .build();

        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(validInvitation));
        when(guestRepository.findById(guestId)).thenReturn(Optional.of(validGuest));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(validEvent));
        when(deliveryLogRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(existingSentLog));

        DeliveryRequestDto request = DeliveryRequestDto.builder()
                .invitationId(invitationId)
                .channels(List.of(DeliveryChannel.WHATSAPP))
                .build();

        MultiChannelDeliveryResponseDto response = deliveryService.sendInvitation(request);

        assertNotNull(response);
        assertEquals(DeliveryStatus.SENT, response.getOverallStatus());
        assertEquals("wamid.HBgL-EXISTING-WA-999", response.getResults().get(0).getProviderReference());
        verifyNoInteractions(mockHttpClient);
    }

    @Test
    @DisplayName("8. Authorization / Cross-event guest rejection")
    void test8_CrossEventRejection() {
        UUID mismatchedEventId = UUID.randomUUID();
        Guest mismatchedGuest = Guest.builder()
                .id(guestId)
                .eventId(mismatchedEventId)
                .fullName("Foreign Guest")
                .phone("0712999888")
                .build();

        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(validInvitation));
        when(guestRepository.findById(guestId)).thenReturn(Optional.of(mismatchedGuest));

        DeliveryRequestDto request = DeliveryRequestDto.builder()
                .invitationId(invitationId)
                .channels(List.of(DeliveryChannel.WHATSAPP))
                .build();

        assertThrows(IllegalArgumentException.class, () -> deliveryService.sendInvitation(request));
        verifyNoInteractions(mockHttpClient);
    }

    @Test
    @DisplayName("9. Delivery record validation — audit fields captured on successful send")
    void test9_DeliveryRecordValidation() throws Exception {
        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(validInvitation));
        when(guestRepository.findById(guestId)).thenReturn(Optional.of(validGuest));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(validEvent));

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("{\"messaging_product\":\"whatsapp\",\"messages\":[{\"id\":\"wamid.AUDIT-WA-777\"}]}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);

        DeliveryRequestDto request = DeliveryRequestDto.builder()
                .invitationId(invitationId)
                .channels(List.of(DeliveryChannel.WHATSAPP))
                .build();

        deliveryService.sendInvitation(request);

        verify(deliveryLogRepository, atLeastOnce()).save(deliveryLogCaptor.capture());
        List<DeliveryLog> savedLogs = deliveryLogCaptor.getAllValues();
        DeliveryLog lastLog = savedLogs.get(savedLogs.size() - 1);

        assertEquals(invitationId, lastLog.getInvitationId());
        assertEquals(guestId, lastLog.getGuestId());
        assertEquals("WHATSAPP", lastLog.getChannel());
        assertEquals("SENT", lastLog.getStatus());
        assertEquals("+255712999888", lastLog.getRecipientContact());
        assertEquals("wamid.AUDIT-WA-777", lastLog.getProviderReference());
        assertNotNull(lastLog.getSentAt());
    }

    @Test
    @DisplayName("10. Retry behavior — retrying a failed WhatsApp delivery log succeeds")
    void test10_RetryBehavior() throws Exception {
        UUID logId = UUID.randomUUID();
        DeliveryLog failedLog = DeliveryLog.builder()
                .id(logId)
                .invitationId(invitationId)
                .guestId(guestId)
                .channel("WHATSAPP")
                .status("FAILED")
                .recipientContact("+255712999888")
                .retryCount(0)
                .idempotencyKey("DELIVERY:" + invitationId + ":WHATSAPP")
                .build();

        when(deliveryLogRepository.findById(logId)).thenReturn(Optional.of(failedLog));
        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(validInvitation));
        when(guestRepository.findById(guestId)).thenReturn(Optional.of(validGuest));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(validEvent));

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("{\"messaging_product\":\"whatsapp\",\"messages\":[{\"id\":\"wamid.RETRY-SUCCESS-999\"}]}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);

        DeliveryLogResponseDto retryResp = deliveryService.retryDelivery(logId);

        assertNotNull(retryResp);
        verify(mockHttpClient, times(1)).send(any(), any());
        assertTrue(failedLog.getRetryCount() >= 1);
    }

    @Test
    @DisplayName("11. Evolution TEST — POST /message/sendText/{instance} is SENT, never DELIVERED")
    void test11_EvolutionSendAcceptedIsSent() throws Exception {
        ReflectionTestUtils.setField(whatsAppChannelProvider, "providerMode", "evolution");
        ReflectionTestUtils.setField(whatsAppChannelProvider, "evolutionBaseUrl", "http://127.0.0.1:8081");
        ReflectionTestUtils.setField(whatsAppChannelProvider, "evolutionInstance", "inviteflow-test");
        ReflectionTestUtils.setField(whatsAppChannelProvider, "evolutionApiKey", "evo-key");

        DeliveryRequest req = DeliveryRequest.builder()
                .invitationId(invitationId)
                .guestName("Amani Neema")
                .eventName("Wedding")
                .recipientPhone("+255712999888")
                .channel(DeliveryChannel.WHATSAPP)
                .build();

        when(mockHttpResponse.statusCode()).thenReturn(201);
        when(mockHttpResponse.body()).thenReturn("{\"key\":{\"remoteJid\":\"255712999888@s.whatsapp.net\",\"fromMe\":true,\"id\":\"BAE594145F4C59B4\"},\"status\":\"PENDING\"}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);

        DeliveryResult result = whatsAppChannelProvider.send(req);

        ArgumentCaptor<HttpRequest> httpCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient).send(httpCaptor.capture(), any(HttpResponse.BodyHandler.class));
        HttpRequest sent = httpCaptor.getValue();
        assertEquals("http://127.0.0.1:8081/message/sendText/inviteflow-test", sent.uri().toString());
        assertEquals("evo-key", sent.headers().firstValue("apikey").orElse(""));

        assertTrue(result.isSuccess());
        assertEquals(DeliveryStatus.SENT, result.getStatus());
        assertNotEquals(DeliveryStatus.DELIVERED, result.getStatus());
        assertEquals("BAE594145F4C59B4", result.getProviderReference());
    }

    @Test
    @DisplayName("11b. Evolution TEST — pressed card PNG is sent as sendMedia")
    void test11b_EvolutionSendsCardImage() throws Exception {
        ReflectionTestUtils.setField(whatsAppChannelProvider, "providerMode", "evolution");
        ReflectionTestUtils.setField(whatsAppChannelProvider, "evolutionBaseUrl", "http://127.0.0.1:8081");
        ReflectionTestUtils.setField(whatsAppChannelProvider, "evolutionInstance", "inviteflow-test");
        ReflectionTestUtils.setField(whatsAppChannelProvider, "evolutionApiKey", "evo-key");

        DeliveryRequest req = DeliveryRequest.builder()
                .invitationId(invitationId)
                .guestName("Amani Neema")
                .eventName("Wedding")
                .recipientPhone("+255712999888")
                .cardImageBytes(new byte[] {1, 2, 3, 4})
                .cardImageFileName("invitation-card.png")
                .channel(DeliveryChannel.WHATSAPP)
                .build();

        when(mockHttpResponse.statusCode()).thenReturn(201);
        when(mockHttpResponse.body()).thenReturn("{\"key\":{\"id\":\"BAE5-IMG-1\"},\"status\":\"PENDING\"}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);

        DeliveryResult result = whatsAppChannelProvider.send(req);

        ArgumentCaptor<HttpRequest> httpCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient).send(httpCaptor.capture(), any(HttpResponse.BodyHandler.class));
        assertEquals("http://127.0.0.1:8081/message/sendMedia/inviteflow-test", httpCaptor.getValue().uri().toString());
        assertTrue(result.isSuccess());
        assertEquals(DeliveryStatus.SENT, result.getStatus());
        assertEquals("BAE5-IMG-1", result.getProviderReference());
    }

    @Test
    @DisplayName("12. Evolution TEST — missing instance/key fails without HTTP")
    void test12_EvolutionMissingConfig() {
        ReflectionTestUtils.setField(whatsAppChannelProvider, "providerMode", "evolution");
        ReflectionTestUtils.setField(whatsAppChannelProvider, "evolutionInstance", "");
        ReflectionTestUtils.setField(whatsAppChannelProvider, "evolutionApiKey", "");

        DeliveryRequest req = DeliveryRequest.builder()
                .invitationId(invitationId)
                .recipientPhone("+255712999888")
                .channel(DeliveryChannel.WHATSAPP)
                .build();

        DeliveryResult result = whatsAppChannelProvider.send(req);

        assertFalse(result.isSuccess());
        assertEquals(DeliveryStatus.FAILED, result.getStatus());
        assertTrue(result.getErrorMessage().contains("instance and API key"));
        verifyNoInteractions(mockHttpClient);
    }

    @Test
    @DisplayName("13. Meta Graph URL substitutes phone-number-id for /me/messages")
    void test13_MetaUsesPhoneNumberId() throws Exception {
        ReflectionTestUtils.setField(whatsAppChannelProvider, "whatsappApiUrl", "https://graph.facebook.com/v18.0/me/messages");
        ReflectionTestUtils.setField(whatsAppChannelProvider, "phoneNumberId", "123456789012345");

        DeliveryRequest req = DeliveryRequest.builder()
                .invitationId(invitationId)
                .recipientPhone("+255712999888")
                .channel(DeliveryChannel.WHATSAPP)
                .build();

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("{\"messages\":[{\"id\":\"wamid.PHONE-ID\"}]}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);

        whatsAppChannelProvider.send(req);

        ArgumentCaptor<HttpRequest> httpCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient).send(httpCaptor.capture(), any(HttpResponse.BodyHandler.class));
        assertEquals("https://graph.facebook.com/v18.0/123456789012345/messages", httpCaptor.getValue().uri().toString());
    }
}
