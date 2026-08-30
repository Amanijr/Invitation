package com.InvitationSystem.InvitationSystem.service;

import com.InvitationSystem.InvitationSystem.Dto.deliveryDto.BatchDeliveryRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.deliveryDto.BatchDeliveryResponseDto;
import com.InvitationSystem.InvitationSystem.Dto.deliveryDto.DeliveryRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.deliveryDto.MultiChannelDeliveryResponseDto;
import com.InvitationSystem.InvitationSystem.entity.*;
import com.InvitationSystem.InvitationSystem.provider.DeliveryRequest;
import com.InvitationSystem.InvitationSystem.provider.DeliveryResult;
import com.InvitationSystem.InvitationSystem.provider.impl.EmailChannelProvider;
import com.InvitationSystem.InvitationSystem.repository.DeliveryLogRepository;
import com.InvitationSystem.InvitationSystem.repository.EventRepository;
import com.InvitationSystem.InvitationSystem.repository.GuestRepository;
import com.InvitationSystem.InvitationSystem.repository.InvitationRepository;
import com.InvitationSystem.InvitationSystem.service.impl.MultiChannelDeliveryServiceImpl;
import com.InvitationSystem.InvitationSystem.util.EmailService;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailDeliveryPhase8Test {

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
    private EmailService emailService;

    @Captor
    private ArgumentCaptor<DeliveryLog> deliveryLogCaptor;

    private EmailChannelProvider emailChannelProvider;
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

        emailChannelProvider = new EmailChannelProvider();
        ReflectionTestUtils.setField(emailChannelProvider, "emailService", emailService);

        deliveryService = new MultiChannelDeliveryServiceImpl(List.of(emailChannelProvider));
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
                .fullName("John Mwita")
                .email("john.mwita@example.com")
                .phone("+255712345678")
                .build();

        validEvent = Event.builder()
                .id(eventId)
                .eventName("Amani & Neema Wedding")
                .eventDate(LocalDateTime.now().plusDays(14))
                .venue("Serena Hotel Ballroom")
                .build();

        validInvitation = Invitation.builder()
                .id(invitationId)
                .eventId(eventId)
                .guestId(guestId)
                .recipientEmail("john.mwita@example.com")
                .recipientPhone("+255712345678")
                .uniqueToken("token-amani-neema-999")
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
    @DisplayName("1. Valid email — invocation succeeds and delivery is recorded as SENT")
    void test1_ValidEmailDelivery() {
        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(validInvitation));
        when(guestRepository.findById(guestId)).thenReturn(Optional.of(validGuest));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(validEvent));

        DeliveryRequestDto request = DeliveryRequestDto.builder()
                .invitationId(invitationId)
                .channels(List.of(DeliveryChannel.EMAIL))
                .build();

        MultiChannelDeliveryResponseDto response = deliveryService.sendInvitation(request);

        assertNotNull(response);
        assertEquals(DeliveryStatus.SENT, response.getOverallStatus());
        assertEquals(1, response.getResults().size());
        assertEquals(DeliveryStatus.SENT, response.getResults().get(0).getStatus());
        verify(emailService, times(1)).sendHtmlEmail(eq("john.mwita@example.com"), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("2. Missing email — request rejected safely and recorded as FAILED")
    void test2_MissingEmailRejection() {
        validGuest.setEmail(null);
        validInvitation.setRecipientEmail(null);

        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(validInvitation));
        when(guestRepository.findById(guestId)).thenReturn(Optional.of(validGuest));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(validEvent));

        DeliveryRequestDto request = DeliveryRequestDto.builder()
                .invitationId(invitationId)
                .channels(List.of(DeliveryChannel.EMAIL))
                .build();

        MultiChannelDeliveryResponseDto response = deliveryService.sendInvitation(request);

        assertNotNull(response);
        assertEquals(DeliveryStatus.FAILED, response.getOverallStatus());
        assertEquals(DeliveryStatus.FAILED, response.getResults().get(0).getStatus());
        assertTrue(response.getResults().get(0).getErrorMessage().contains("missing or empty"));
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("3. Invalid email — validation failure recorded as FAILED")
    void test3_InvalidEmailFormat() {
        validInvitation.setRecipientEmail("invalid-email-address");

        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(validInvitation));
        when(guestRepository.findById(guestId)).thenReturn(Optional.of(validGuest));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(validEvent));

        DeliveryRequestDto request = DeliveryRequestDto.builder()
                .invitationId(invitationId)
                .channels(List.of(DeliveryChannel.EMAIL))
                .recipientEmail("invalid-email-address")
                .build();

        MultiChannelDeliveryResponseDto response = deliveryService.sendInvitation(request);

        assertNotNull(response);
        assertEquals(DeliveryStatus.FAILED, response.getOverallStatus());
        assertTrue(response.getResults().get(0).getErrorMessage().contains("Invalid recipient email format"));
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("4. Unauthorized admin / invalid request — null invitation ID throws IllegalArgumentException")
    void test4_InvalidRequestHandling() {
        DeliveryRequestDto request = DeliveryRequestDto.builder()
                .invitationId(null)
                .channels(List.of(DeliveryChannel.EMAIL))
                .build();

        assertThrows(IllegalArgumentException.class, () -> deliveryService.sendInvitation(request));
    }

    @Test
    @DisplayName("5. Guest from another event — cross-event delivery rejected")
    void test5_CrossEventDeliveryRejection() {
        UUID otherEventId = UUID.randomUUID();
        Guest mismatchedGuest = Guest.builder()
                .id(guestId)
                .eventId(otherEventId)
                .fullName("Foreign Guest")
                .email("foreign@example.com")
                .build();

        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(validInvitation));
        when(guestRepository.findById(guestId)).thenReturn(Optional.of(mismatchedGuest));

        DeliveryRequestDto request = DeliveryRequestDto.builder()
                .invitationId(invitationId)
                .channels(List.of(DeliveryChannel.EMAIL))
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> deliveryService.sendInvitation(request));
        assertTrue(ex.getMessage().contains("does not belong to the specified event"));
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("6. Invalid invitation — non-existent ID rejected safely")
    void test6_InvalidInvitationID() {
        UUID nonExistentId = UUID.randomUUID();
        when(invitationRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        DeliveryRequestDto request = DeliveryRequestDto.builder()
                .invitationId(nonExistentId)
                .channels(List.of(DeliveryChannel.EMAIL))
                .build();

        assertThrows(IllegalArgumentException.class, () -> deliveryService.sendInvitation(request));
    }

    @Test
    @DisplayName("7. SMTP failure — exception caught and delivery status recorded as FAILED")
    void test7_SMTPFailureHandling() {
        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(validInvitation));
        when(guestRepository.findById(guestId)).thenReturn(Optional.of(validGuest));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(validEvent));

        doThrow(new RuntimeException("SMTP Connection timed out")).when(emailService).sendHtmlEmail(anyString(), anyString(), anyString(), anyString());

        DeliveryRequestDto request = DeliveryRequestDto.builder()
                .invitationId(invitationId)
                .channels(List.of(DeliveryChannel.EMAIL))
                .build();

        MultiChannelDeliveryResponseDto response = deliveryService.sendInvitation(request);

        assertNotNull(response);
        assertEquals(DeliveryStatus.FAILED, response.getOverallStatus());
        assertEquals(DeliveryStatus.FAILED, response.getResults().get(0).getStatus());
        assertTrue(response.getResults().get(0).getErrorMessage().contains("SMTP Connection timed out"));
    }

    @Test
    @DisplayName("8. Duplicate send — idempotency policy prevents duplicate email dispatch")
    void test8_DuplicateSendProtection() {
        String key = "DELIVERY:" + invitationId + ":EMAIL";
        DeliveryLog existingSentLog = DeliveryLog.builder()
                .id(UUID.randomUUID())
                .invitationId(invitationId)
                .channel("EMAIL")
                .status("SENT")
                .idempotencyKey(key)
                .providerReference("EMAIL-PREVIOUS-123")
                .sentAt(LocalDateTime.now().minusMinutes(10))
                .build();

        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(validInvitation));
        when(guestRepository.findById(guestId)).thenReturn(Optional.of(validGuest));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(validEvent));
        when(deliveryLogRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(existingSentLog));

        DeliveryRequestDto request = DeliveryRequestDto.builder()
                .invitationId(invitationId)
                .channels(List.of(DeliveryChannel.EMAIL))
                .build();

        MultiChannelDeliveryResponseDto response = deliveryService.sendInvitation(request);

        assertNotNull(response);
        assertEquals(DeliveryStatus.SENT, response.getOverallStatus());
        assertEquals("EMAIL-PREVIOUS-123", response.getResults().get(0).getProviderReference());
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("9. Partial bulk failure — 10 invitations: 8 successful, 2 failed")
    void test9_PartialBulkFailureHandling() {
        List<UUID> invitationIds = new ArrayList<>();
        List<Invitation> mockInvitations = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            UUID invId = UUID.randomUUID();
            UUID gId = UUID.randomUUID();
            invitationIds.add(invId);

            boolean isFailureCase = (i == 3 || i == 7);
            String email = isFailureCase ? "invalid-email-format" : ("guest" + i + "@example.com");

            Guest g = Guest.builder()
                    .id(gId)
                    .eventId(eventId)
                    .fullName("Guest " + i)
                    .email(email)
                    .build();

            Invitation inv = Invitation.builder()
                    .id(invId)
                    .eventId(eventId)
                    .guestId(gId)
                    .recipientEmail(email)
                    .uniqueToken("token-" + i)
                    .status(InvitationStatus.GENERATED)
                    .build();

            mockInvitations.add(inv);
            when(invitationRepository.findById(invId)).thenReturn(Optional.of(inv));
            when(guestRepository.findById(gId)).thenReturn(Optional.of(g));
        }

        when(invitationRepository.findAllById(invitationIds)).thenReturn(mockInvitations);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(validEvent));

        BatchDeliveryRequestDto batchReq = BatchDeliveryRequestDto.builder()
                .invitationIds(invitationIds)
                .channels(List.of(DeliveryChannel.EMAIL))
                .idempotencyPrefix("BULK-TEST-BATCH")
                .build();

        BatchDeliveryResponseDto batchResp = deliveryService.sendBatchInvitations(batchReq);

        assertNotNull(batchResp);
        assertEquals(10, batchResp.getTotalInvitations());
        assertEquals(8, batchResp.getSuccessCount());
        assertEquals(2, batchResp.getFailedCount());
        assertEquals(10, batchResp.getInvitationResults().size());
    }

    @Test
    @DisplayName("10. Email content personalization — addresses guest by name and contains secure URL")
    void test10_EmailContentPersonalization() {
        DeliveryRequest deliveryReq = DeliveryRequest.builder()
                .invitationId(invitationId)
                .guestId(guestId)
                .guestName("John Mwita")
                .eventName("Amani & Neema Wedding")
                .invitationToken("token-amani-neema-999")
                .invitationUrl("http://localhost:8080/api/v1/invitations/token/token-amani-neema-999")
                .recipientEmail("john.mwita@example.com")
                .channel(DeliveryChannel.EMAIL)
                .build();

        DeliveryResult result = emailChannelProvider.send(deliveryReq);

        assertTrue(result.isSuccess());
        assertEquals(DeliveryStatus.SENT, result.getStatus());

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);

        verify(emailService).sendHtmlEmail(eq("john.mwita@example.com"), subjectCaptor.capture(), anyString(), htmlCaptor.capture());

        assertEquals("Your invitation to Amani & Neema Wedding", subjectCaptor.getValue());
        assertTrue(htmlCaptor.getValue().contains("John Mwita"));
        assertTrue(htmlCaptor.getValue().contains("Amani &amp; Neema Wedding"));
        assertFalse(htmlCaptor.getValue().contains("/api/v1/invitations"));
    }

    @Test
    @DisplayName("Catalog JSON metadata is not used as the email body")
    void catalogJson_isNotEmailedAsBody() {
        DeliveryRequest deliveryReq = DeliveryRequest.builder()
                .invitationId(invitationId)
                .guestName("John Mwita")
                .eventName("Amani & Neema Wedding")
                .invitationUrl("http://localhost:8080/api/v1/invitations/token/token-amani-neema-999")
                .recipientEmail("john.mwita@example.com")
                .renderedHtml("{\"template_title\": \"Floral Oval Wedding Invitation\", \"license\": \"CC BY 4.0\"}")
                .channel(DeliveryChannel.EMAIL)
                .build();

        DeliveryResult result = emailChannelProvider.send(deliveryReq);

        assertTrue(result.isSuccess());
        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendHtmlEmail(eq("john.mwita@example.com"), anyString(), anyString(), htmlCaptor.capture());
        assertFalse(htmlCaptor.getValue().contains("template_title"));
        assertTrue(htmlCaptor.getValue().contains("John Mwita"));
    }

    @Test
    @DisplayName("Pressed card PNG is inlined and attached")
    void pressedCardPng_isSentWithEmail() {
        byte[] png = new byte[] {1, 2, 3, 4};
        DeliveryRequest deliveryReq = DeliveryRequest.builder()
                .invitationId(invitationId)
                .guestName("John Mwita")
                .eventName("Gold wedding")
                .invitationUrl("http://localhost:8080/invite")
                .recipientEmail("john.mwita@example.com")
                .cardImageBytes(png)
                .cardImageFileName("invitation-card.png")
                .channel(DeliveryChannel.EMAIL)
                .build();

        DeliveryResult result = emailChannelProvider.send(deliveryReq);

        assertTrue(result.isSuccess());
        verify(emailService).sendHtmlEmailWithCard(
                eq("john.mwita@example.com"),
                eq("Your invitation to Gold wedding"),
                anyString(),
                argThat(html -> html.contains("cid:invitation-card") && html.contains("John Mwita")),
                eq(png),
                eq("invitation-card.png")
        );
        verify(emailService, never()).sendHtmlEmail(anyString(), anyString(), anyString(), anyString());
    }
}
