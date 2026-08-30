package com.InvitationSystem.InvitationSystem.service;

import com.InvitationSystem.InvitationSystem.Dto.deliveryDto.BatchDeliveryRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.deliveryDto.BatchDeliveryResponseDto;
import com.InvitationSystem.InvitationSystem.Dto.deliveryDto.DeliveryRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.deliveryDto.MultiChannelDeliveryResponseDto;
import com.InvitationSystem.InvitationSystem.Dto.deliveryLogsDto.DeliveryLogResponseDto;
import com.InvitationSystem.InvitationSystem.entity.*;
import com.InvitationSystem.InvitationSystem.provider.ChannelProvider;
import com.InvitationSystem.InvitationSystem.provider.DeliveryRequest;
import com.InvitationSystem.InvitationSystem.provider.DeliveryResult;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MultiChannelDeliveryServiceTest {

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
    private ChannelProvider emailProvider;

    @Mock
    private ChannelProvider smsProvider;

    @Mock
    private ChannelProvider whatsAppProvider;

    @Captor
    private ArgumentCaptor<DeliveryLog> deliveryLogCaptor;

    private MultiChannelDeliveryServiceImpl deliveryService;

    private UUID invitationId;
    private UUID guestId;
    private UUID eventId;
    private Invitation testInvitation;
    private Guest testGuest;
    private Event testEvent;

    @BeforeEach
    void setUp() {
        invitationId = UUID.randomUUID();
        guestId = UUID.randomUUID();
        eventId = UUID.randomUUID();

        when(emailProvider.getChannel()).thenReturn(DeliveryChannel.EMAIL);
        when(smsProvider.getChannel()).thenReturn(DeliveryChannel.SMS);
        when(whatsAppProvider.getChannel()).thenReturn(DeliveryChannel.WHATSAPP);

        deliveryService = new MultiChannelDeliveryServiceImpl(List.of(emailProvider, smsProvider, whatsAppProvider));

        // Inject mocks via reflection or spring context
        org.springframework.test.util.ReflectionTestUtils.setField(deliveryService, "invitationRepository", invitationRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(deliveryService, "guestRepository", guestRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(deliveryService, "eventRepository", eventRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(deliveryService, "deliveryLogRepository", deliveryLogRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(deliveryService, "eCardRenderingEngineService", eCardRenderingEngineService);
        org.springframework.test.util.ReflectionTestUtils.setField(deliveryService, "templateProcessorService", templateProcessorService);
        org.springframework.test.util.ReflectionTestUtils.setField(deliveryService, "pdfService", pdfService);
        org.springframework.test.util.ReflectionTestUtils.setField(deliveryService, "publicUrl", "http://localhost:5173");

        testGuest = Guest.builder()
                .id(guestId)
                .eventId(eventId)
                .fullName("John Doe")
                .email("john.doe@example.com")
                .phone("+1234567890")
                .build();

        testEvent = Event.builder()
                .id(eventId)
                .eventName("Annual Gala")
                .eventDate(LocalDateTime.now().plusDays(10))
                .venue("Grand Hotel")
                .build();

        testInvitation = Invitation.builder()
                .id(invitationId)
                .eventId(eventId)
                .guestId(guestId)
                .recipientEmail("john.doe@example.com")
                .recipientPhone("+1234567890")
                .uniqueToken("token-12345")
                .qrCodeUrl("http://localhost:8080/scan/token-12345")
                .qrCode("base64qrcode")
                .status(InvitationStatus.GENERATED)
                .deliveryStatus(DeliveryStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("1. Valid delivery across Email, SMS, and WhatsApp")
    void testValidMultiChannelDelivery() {
        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(testInvitation));
        when(guestRepository.findById(guestId)).thenReturn(Optional.of(testGuest));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));

        when(emailProvider.send(any(DeliveryRequest.class))).thenReturn(DeliveryResult.builder()
                .success(true)
                .status(DeliveryStatus.SENT)
                .recipientContact("john.doe@example.com")
                .providerReference("EMAIL-123")
                .providerResponse("Email Sent")
                .build());

        when(smsProvider.send(any(DeliveryRequest.class))).thenReturn(DeliveryResult.builder()
                .success(true)
                .status(DeliveryStatus.SENT)
                .recipientContact("+1234567890")
                .providerReference("SMS-123")
                .providerResponse("SMS Sent")
                .build());

        when(whatsAppProvider.send(any(DeliveryRequest.class))).thenReturn(DeliveryResult.builder()
                .success(true)
                .status(DeliveryStatus.SENT)
                .recipientContact("+1234567890")
                .providerReference("WA-123")
                .providerResponse("WhatsApp Sent")
                .build());

        when(deliveryLogRepository.save(any(DeliveryLog.class))).thenAnswer(invocation -> {
            DeliveryLog log = invocation.getArgument(0);
            if (log.getId() == null) log.setId(UUID.randomUUID());
            return log;
        });

        DeliveryRequestDto request = DeliveryRequestDto.builder()
                .invitationId(invitationId)
                .channels(List.of(DeliveryChannel.EMAIL, DeliveryChannel.SMS, DeliveryChannel.WHATSAPP))
                .build();

        MultiChannelDeliveryResponseDto response = deliveryService.sendInvitation(request);

        assertNotNull(response);
        assertEquals(invitationId, response.getInvitationId());
        assertEquals(DeliveryStatus.SENT, response.getOverallStatus());
        assertEquals(3, response.getResults().size());

        verify(smsProvider, times(1)).send(any(DeliveryRequest.class));
        ArgumentCaptor<DeliveryRequest> delivered = ArgumentCaptor.forClass(DeliveryRequest.class);
        verify(emailProvider).send(delivered.capture());
        assertEquals("http://localhost:5173/invite/token-12345", delivered.getValue().getInvitationUrl());
    }

    @Test
    @DisplayName("2. Missing contact information for requested channel")
    void testMissingContactInformation() {
        testInvitation.setRecipientPhone(null);
        testGuest.setPhone(null);

        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(testInvitation));
        when(guestRepository.findById(guestId)).thenReturn(Optional.of(testGuest));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));

        when(smsProvider.send(any(DeliveryRequest.class))).thenReturn(DeliveryResult.builder()
                .success(false)
                .status(DeliveryStatus.FAILED)
                .errorMessage("Recipient phone number is missing or empty")
                .build());

        when(deliveryLogRepository.save(any(DeliveryLog.class))).thenAnswer(i -> {
            DeliveryLog log = i.getArgument(0);
            if (log.getId() == null) log.setId(UUID.randomUUID());
            return log;
        });

        DeliveryRequestDto request = DeliveryRequestDto.builder()
                .invitationId(invitationId)
                .channels(List.of(DeliveryChannel.SMS))
                .build();

        MultiChannelDeliveryResponseDto response = deliveryService.sendInvitation(request);

        assertNotNull(response);
        assertEquals(DeliveryStatus.FAILED, response.getOverallStatus());
        assertFalse(response.getResults().get(0).getErrorMessage().isEmpty());
    }

    @Test
    @DisplayName("3. Duplicate delivery idempotency check")
    void testDuplicateDeliveryIdempotency() {
        String key = "DELIVERY:" + invitationId + ":EMAIL";

        DeliveryLog existingLog = DeliveryLog.builder()
                .id(UUID.randomUUID())
                .invitationId(invitationId)
                .channel("EMAIL")
                .status("SENT")
                .idempotencyKey(key)
                .providerReference("EMAIL-EXISTING")
                .sentAt(LocalDateTime.now().minusMinutes(5))
                .build();

        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(testInvitation));
        when(guestRepository.findById(guestId)).thenReturn(Optional.of(testGuest));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
        when(deliveryLogRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(existingLog));

        DeliveryRequestDto request = DeliveryRequestDto.builder()
                .invitationId(invitationId)
                .channels(List.of(DeliveryChannel.EMAIL))
                .build();

        MultiChannelDeliveryResponseDto response = deliveryService.sendInvitation(request);

        assertNotNull(response);
        assertEquals(1, response.getResults().size());
        assertEquals("EMAIL-EXISTING", response.getResults().get(0).getProviderReference());

        // Verify provider send was NEVER called because of idempotency deduplication!
        verify(emailProvider, never()).send(any());
    }

    @Test
    @DisplayName("4. Provider failure handling")
    void testProviderFailureHandling() {
        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(testInvitation));
        when(guestRepository.findById(guestId)).thenReturn(Optional.of(testGuest));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));

        when(emailProvider.send(any(DeliveryRequest.class))).thenReturn(DeliveryResult.builder()
                .success(false)
                .status(DeliveryStatus.FAILED)
                .recipientContact("john.doe@example.com")
                .errorMessage("SMTP Authentication Failed")
                .build());

        when(deliveryLogRepository.save(any(DeliveryLog.class))).thenAnswer(i -> {
            DeliveryLog log = i.getArgument(0);
            if (log.getId() == null) log.setId(UUID.randomUUID());
            return log;
        });

        DeliveryRequestDto request = DeliveryRequestDto.builder()
                .invitationId(invitationId)
                .channels(List.of(DeliveryChannel.EMAIL))
                .build();

        MultiChannelDeliveryResponseDto response = deliveryService.sendInvitation(request);

        assertEquals(DeliveryStatus.FAILED, response.getOverallStatus());
        assertEquals("SMTP Authentication Failed", response.getResults().get(0).getErrorMessage());
        assertEquals(InvitationStatus.FAILED, testInvitation.getStatus());
    }

    @Test
    @DisplayName("5. Retry behavior for failed delivery record")
    void testRetryBehavior() {
        UUID logId = UUID.randomUUID();
        DeliveryLog failedLog = DeliveryLog.builder()
                .id(logId)
                .invitationId(invitationId)
                .guestId(guestId)
                .channel("EMAIL")
                .status("FAILED")
                .retryCount(0)
                .idempotencyKey("DELIVERY:" + invitationId + ":EMAIL")
                .build();

        when(deliveryLogRepository.findById(logId)).thenReturn(Optional.of(failedLog));
        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(testInvitation));
        when(guestRepository.findById(guestId)).thenReturn(Optional.of(testGuest));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));

        when(emailProvider.send(any(DeliveryRequest.class))).thenReturn(DeliveryResult.builder()
                .success(true)
                .status(DeliveryStatus.SENT)
                .recipientContact("john.doe@example.com")
                .providerReference("EMAIL-RETRY-SUCCESS")
                .build());

        when(deliveryLogRepository.save(any(DeliveryLog.class))).thenAnswer(i -> i.getArgument(0));

        DeliveryLogResponseDto response = deliveryService.retryDelivery(logId);

        assertNotNull(response);
        verify(emailProvider, times(1)).send(any());
        assertTrue(failedLog.getRetryCount() >= 1);
    }

    @Test
    @DisplayName("6. Delivery record creation and auditing fields")
    void testDeliveryRecordCreation() {
        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(testInvitation));
        when(guestRepository.findById(guestId)).thenReturn(Optional.of(testGuest));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));

        when(emailProvider.send(any(DeliveryRequest.class))).thenReturn(DeliveryResult.builder()
                .success(true)
                .status(DeliveryStatus.SENT)
                .recipientContact("john.doe@example.com")
                .providerReference("EMAIL-REF-999")
                .providerResponse("200 OK")
                .build());

        when(deliveryLogRepository.save(deliveryLogCaptor.capture())).thenAnswer(i -> {
            DeliveryLog log = i.getArgument(0);
            if (log.getId() == null) log.setId(UUID.randomUUID());
            return log;
        });

        DeliveryRequestDto request = DeliveryRequestDto.builder()
                .invitationId(invitationId)
                .channels(List.of(DeliveryChannel.EMAIL))
                .build();

        deliveryService.sendInvitation(request);

        List<DeliveryLog> savedLogs = deliveryLogCaptor.getAllValues();
        assertFalse(savedLogs.isEmpty());
        DeliveryLog finalLog = savedLogs.get(savedLogs.size() - 1);

        assertEquals(invitationId, finalLog.getInvitationId());
        assertEquals(guestId, finalLog.getGuestId());
        assertEquals("EMAIL", finalLog.getChannel());
        assertEquals("SENT", finalLog.getStatus());
        assertEquals("EMAIL-REF-999", finalLog.getProviderReference());
        assertNotNull(finalLog.getSentAt());
    }

    @Test
    @DisplayName("7. Batch delivery sending across invitations")
    void testBatchDelivery() {
        UUID invitationId2 = UUID.randomUUID();
        Invitation testInvitation2 = Invitation.builder()
                .id(invitationId2)
                .eventId(eventId)
                .guestId(guestId)
                .recipientEmail("john2@example.com")
                .uniqueToken("token-222")
                .status(InvitationStatus.GENERATED)
                .build();

        when(invitationRepository.findAllById(any())).thenReturn(List.of(testInvitation, testInvitation2));
        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(testInvitation));
        when(invitationRepository.findById(invitationId2)).thenReturn(Optional.of(testInvitation2));
        when(guestRepository.findById(guestId)).thenReturn(Optional.of(testGuest));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));

        when(emailProvider.send(any(DeliveryRequest.class))).thenReturn(DeliveryResult.builder()
                .success(true)
                .status(DeliveryStatus.SENT)
                .providerReference("BATCH-REF")
                .build());

        when(deliveryLogRepository.save(any(DeliveryLog.class))).thenAnswer(i -> {
            DeliveryLog log = i.getArgument(0);
            if (log.getId() == null) log.setId(UUID.randomUUID());
            return log;
        });

        BatchDeliveryRequestDto batchRequest = BatchDeliveryRequestDto.builder()
                .invitationIds(List.of(invitationId, invitationId2))
                .channels(List.of(DeliveryChannel.EMAIL))
                .idempotencyPrefix("TEST-BATCH-PREFIX")
                .build();

        BatchDeliveryResponseDto response = deliveryService.sendBatchInvitations(batchRequest);

        assertNotNull(response);
        assertEquals(2, response.getTotalInvitations());
        assertEquals(2, response.getSuccessCount());
        assertEquals(0, response.getFailedCount());
    }

    @Test
    @DisplayName("All logs include the guest name for the desk")
    void getAllLogs_includesGuestName() {
        DeliveryLog log = DeliveryLog.builder()
                .id(UUID.randomUUID())
                .invitationId(invitationId)
                .guestId(guestId)
                .channel("EMAIL")
                .status("FAILED")
                .recipientContact("john.doe@example.com")
                .retryCount(0)
                .build();
        when(deliveryLogRepository.findAll()).thenReturn(List.of(log));
        when(guestRepository.findAllById(any())).thenReturn(List.of(testGuest));

        List<DeliveryLogResponseDto> logs = deliveryService.getAllLogs();

        assertEquals(1, logs.size());
        assertEquals("John Doe", logs.get(0).getGuestName());
        assertEquals("john.doe@example.com", logs.get(0).getRecipientContact());
        assertEquals("FAILED", logs.get(0).getStatus());
    }
}
