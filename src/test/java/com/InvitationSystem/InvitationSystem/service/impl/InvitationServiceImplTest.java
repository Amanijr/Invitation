package com.InvitationSystem.InvitationSystem.service.impl;

import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationDetailedResponseDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationResponseDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationScanResponseDto;
import com.InvitationSystem.InvitationSystem.entity.CheckIn;
import com.InvitationSystem.InvitationSystem.entity.DeliveryStatus;
import com.InvitationSystem.InvitationSystem.entity.Event;
import com.InvitationSystem.InvitationSystem.entity.EventType;
import com.InvitationSystem.InvitationSystem.entity.Invitation;
import com.InvitationSystem.InvitationSystem.entity.InvitationStatus;
import com.InvitationSystem.InvitationSystem.entity.Template;
import com.InvitationSystem.InvitationSystem.repository.EventRepository;
import com.InvitationSystem.InvitationSystem.repository.InvitationRepository;
import com.InvitationSystem.InvitationSystem.repository.TemplateRepository;
import com.InvitationSystem.InvitationSystem.util.EmailService;
import com.InvitationSystem.InvitationSystem.util.PDFService;
import com.InvitationSystem.InvitationSystem.util.QRCodeService;
import com.InvitationSystem.InvitationSystem.util.TemplateProcessorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationServiceImplTest {

    @Mock
    private InvitationRepository invitationRepository;
    @Mock
    private TemplateRepository templateRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private com.InvitationSystem.InvitationSystem.repository.GuestRepository guestRepository;
    @Mock
    private com.InvitationSystem.InvitationSystem.repository.CheckInRepository checkInRepository;
    @Mock
    private com.InvitationSystem.InvitationSystem.util.TokenGeneratorService tokenGeneratorService;
    @Mock
    private QRCodeService qrCodeService;
    @Mock
    private EmailService emailService;
    @Mock
    private TemplateProcessorService templateProcessorService;
    @Mock
    private PDFService pdfService;
    @Mock
    private com.InvitationSystem.InvitationSystem.service.ECardRenderingEngineService eCardRenderingEngineService;
    @Mock
    private com.InvitationSystem.InvitationSystem.service.CheckInAuditRecorder checkInAuditRecorder;

    @InjectMocks
    private InvitationServiceImpl invitationService;

    private UUID eventId;
    private UUID guestId;
    private UUID templateId;
    private UUID invitationId;

    private InvitationRequestDto request;
    private Event event;
    private Template template;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(invitationService, "baseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(invitationService, "publicUrl", "http://localhost:5173");

        eventId = UUID.randomUUID();
        guestId = UUID.randomUUID();
        templateId = UUID.randomUUID();
        invitationId = UUID.randomUUID();

        request = new InvitationRequestDto();
        request.setEventId(eventId);
        request.setGuestId(guestId);
        request.setTemplateId(templateId);
        request.setGuestName("Guest One");
        request.setRecipientEmail("guest@example.com");
        request.setRecipientPhone("+1234567890");
        request.setExpiryDate(LocalDateTime.now().plusDays(1));
        request.setAttachPdf(true);

        event = Event.builder()
                .id(eventId)
                .eventName("Tech Conference")
                .eventDate(LocalDateTime.now().plusDays(2))
                .eventType(EventType.CONFERENCE)
                .venue("Main Hall")
                .createdBy(UUID.randomUUID())
                .status("ACTIVE")
                .build();

        template = Template.builder()
                .id(templateId)
                .eventId(eventId)
                .eventType(EventType.CONFERENCE)
                .templateName("Conference Card")
                .content("<html>{{guestName}} {{eventName}} {{eventDate}} {{qrCode}}</html>")
                .active(true)
                .build();

        lenient().when(eCardRenderingEngineService.renderAndStoreCard(any(Invitation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(checkInAuditRecorder.record(any())).thenAnswer(invocation -> {
            CheckIn audit = invocation.getArgument(0);
            if (audit.getId() == null) {
                audit.setId(UUID.randomUUID());
            }
            return audit;
        });
    }

    @Test
    void createInvitation_happyPath_updatesDeliveryAndSentAt() {
        when(invitationRepository.findByEventIdAndGuestId(eventId, guestId)).thenReturn(Optional.empty());
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(qrCodeService.generateQRCodeImage(anyString())).thenReturn("qr-base64");
        when(templateProcessorService.renderTemplate(anyString(), any())).thenReturn("<html>rendered</html>");
        when(pdfService.generateInvitationCardPdf("<html>rendered</html>")).thenReturn("pdf-base64");
        when(tokenGeneratorService.generateSecureToken()).thenReturn("mock-token");
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(invocation -> {
            Invitation inv = invocation.getArgument(0);
            if (inv.getId() == null) {
                inv.setId(invitationId);
            }
            if (inv.getGeneratedAt() == null) {
                inv.setGeneratedAt(LocalDateTime.now());
            }
            return inv;
        });

        InvitationResponseDto response = invitationService.createInvitation(request);

        assertNotNull(response);
        assertEquals(invitationId, response.getId());
        assertEquals(InvitationStatus.SENT, response.getStatus());
        assertEquals(DeliveryStatus.SENT_EMAIL, response.getDeliveryStatus());
        assertNotNull(response.getGeneratedAt());
        verify(emailService, times(1)).sendInvitationEmail(eq("guest@example.com"), anyString(), eq("<html>rendered</html>"), eq("pdf-base64"), anyString());
    }

    @Test
    void createInvitation_duplicateForEventAndGuest_throws() {
        Invitation existing = Invitation.builder().id(UUID.randomUUID()).eventId(eventId).guestId(guestId).build();
        when(invitationRepository.findByEventIdAndGuestId(eventId, guestId)).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class, () -> invitationService.createInvitation(request));
        verify(invitationRepository, never()).save(any());
    }

    @Test
    void createInvitation_invalidEvent_throws() {
        when(invitationRepository.findByEventIdAndGuestId(eventId, guestId)).thenReturn(Optional.empty());
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> invitationService.createInvitation(request));
    }

    @Test
    void createInvitation_missingTemplateContent_usesFallbackHtml() {
        template.setContent("   ");
        when(invitationRepository.findByEventIdAndGuestId(eventId, guestId)).thenReturn(Optional.empty());
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(qrCodeService.generateQRCodeImage(anyString())).thenReturn("qr-base64");
        when(tokenGeneratorService.generateSecureToken()).thenReturn("mock-token");
        when(pdfService.generateInvitationCardPdf(anyString())).thenReturn("pdf-base64");
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InvitationResponseDto response = invitationService.createInvitation(request);
        assertNotNull(response);
        assertEquals(InvitationStatus.SENT, response.getStatus());
    }

    @Test
    void createInvitation_missingContact_throws() {
        request.setRecipientEmail(null);
        request.setRecipientPhone(null);

        assertThrows(IllegalArgumentException.class, () -> invitationService.createInvitation(request));
    }

    @Test
    void createInvitation_invalidEmail_throws() {
        request.setRecipientEmail("not-an-email");

        assertThrows(IllegalArgumentException.class, () -> invitationService.createInvitation(request));
    }

    @Test
    void createInvitation_emailFailure_setsFailedStatus() {
        when(invitationRepository.findByEventIdAndGuestId(eventId, guestId)).thenReturn(Optional.empty());
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(qrCodeService.generateQRCodeImage(anyString())).thenReturn("qr-base64");
        when(templateProcessorService.renderTemplate(anyString(), any())).thenReturn("<html>rendered</html>");
        when(pdfService.generateInvitationCardPdf(anyString())).thenReturn("pdf-base64");
        when(tokenGeneratorService.generateSecureToken()).thenReturn("mock-token");
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.doThrow(new RuntimeException("smtp down"))
                .when(emailService)
                .sendInvitationEmail(anyString(), anyString(), anyString(), anyString(), anyString());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> invitationService.createInvitation(request));
        assertTrue(ex.getMessage().contains("Invitation email sending failed"));
        verify(invitationRepository, times(2)).save(any(Invitation.class));
    }

    @Test
    void generateQrCode_setsQrCodeAndUrl() {
        Invitation invitation = Invitation.builder().id(invitationId).uniqueToken("tok").build();
        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));
        when(qrCodeService.generateQRCodeImage(anyString())).thenReturn("new-qr");
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InvitationDetailedResponseDto dto = invitationService.generateQrCode(invitationId, null);

        assertEquals("new-qr", dto.getQrCode());
        assertNotNull(dto.getQrCodeUrl());
    }

    @Test
    void validateInvitation_rejectsUsedInvitation() {
        Invitation invitation = Invitation.builder()
                .id(invitationId)
                .uniqueToken("tok")
                .recipientEmail("guest@example.com")
                .recipientPhone("+1234567890")
                .used(true)
                .status(InvitationStatus.USED)
                .build();
        when(invitationRepository.findByUniqueToken("tok")).thenReturn(Optional.of(invitation));

        assertThrows(IllegalArgumentException.class, () -> invitationService.validateInvitation("tok", "+1234567890", "guest@example.com"));
    }

    @Test
    void validateInvitation_rejectsExpiredInvitation() {
        Invitation invitation = Invitation.builder()
                .id(invitationId)
                .uniqueToken("tok")
                .recipientEmail("guest@example.com")
                .recipientPhone("+1234567890")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .status(InvitationStatus.SENT)
                .build();
        when(invitationRepository.findByUniqueToken("tok")).thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(IllegalArgumentException.class, () -> invitationService.validateInvitation("tok", "+1234567890", "guest@example.com"));
    }

    @Test
    void scanInvitation_previewDoesNotConsumeAdmission() {
        Invitation invitation = Invitation.builder()
                .id(invitationId)
                .uniqueToken("tok")
                .status(InvitationStatus.SENT)
                .used(false)
                .scanned(false)
                .admissionLimit(1)
                .usedAdmissions(0)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();
        when(invitationRepository.findByUniqueToken("tok")).thenReturn(Optional.of(invitation));

        InvitationScanResponseDto response = invitationService.scanInvitationByToken("tok");

        assertEquals(InvitationStatus.SENT, response.getStatus());
        assertFalse(response.isScanned());
        assertEquals("Invitation valid", response.getMessage());
        verify(invitationRepository, never()).findByUniqueTokenForUpdate("tok");
        verify(invitationRepository, never()).save(any(Invitation.class));
    }

    @Test
    void scanInvitation_alreadyUsed_returnsAlreadyCheckedInWithoutWrite() {
        Invitation invitation = Invitation.builder()
                .id(invitationId)
                .uniqueToken("tok")
                .status(InvitationStatus.USED)
                .used(true)
                .scanned(true)
                .admissionLimit(1)
                .usedAdmissions(1)
                .build();
        when(invitationRepository.findByUniqueToken("tok")).thenReturn(Optional.of(invitation));

        InvitationScanResponseDto response = invitationService.scanInvitationByToken("tok");

        assertEquals("Already checked in", response.getMessage());
        verify(invitationRepository, never()).save(any(Invitation.class));
    }

    @Test
    void integrationStyle_fullFlow_eventTemplateInvitationDelivery() {
        when(invitationRepository.findByEventIdAndGuestId(eventId, guestId)).thenReturn(Optional.empty());
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(qrCodeService.generateQRCodeImage(anyString())).thenReturn("qr-base64");
        when(templateProcessorService.renderTemplate(anyString(), any())).thenAnswer(invocation -> {
            String html = invocation.getArgument(0);
            return html.replace("{{guestName}}", "Guest One")
                    .replace("{{eventName}}", "Tech Conference")
                    .replace("{{eventDate}}", "2026-01-01 10:00")
                    .replace("{{qrCode}}", "<img src='qr' />");
        });
        when(pdfService.generateInvitationCardPdf(anyString())).thenReturn("pdf-base64");
        when(tokenGeneratorService.generateSecureToken()).thenReturn("mock-token");
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(invocation -> {
            Invitation inv = invocation.getArgument(0);
            if (inv.getId() == null) {
                inv.setId(invitationId);
            }
            return inv;
        });

        InvitationResponseDto created = invitationService.createInvitation(request);
        when(invitationRepository.findByUniqueToken(created.getUniqueToken()))
                .thenReturn(Optional.of(Invitation.builder()
                        .id(created.getId())
                        .uniqueToken(created.getUniqueToken())
                        .status(InvitationStatus.SENT)
                        .used(false)
                        .scanned(false)
                        .admissionLimit(1)
                        .usedAdmissions(0)
                        .recipientEmail("guest@example.com")
                        .recipientPhone("+1234567890")
                        .expiresAt(LocalDateTime.now().plusDays(1))
                        .build()));

        InvitationScanResponseDto scan = invitationService.scanInvitationByToken(created.getUniqueToken());

        assertEquals(InvitationStatus.SENT, created.getStatus());
        assertEquals(DeliveryStatus.SENT_EMAIL, created.getDeliveryStatus());
        assertEquals("Invitation valid", scan.getMessage());
        assertEquals(InvitationStatus.SENT, scan.getStatus());
        verify(invitationRepository, never()).findByUniqueTokenForUpdate(created.getUniqueToken());
    }

    @Test
    void generateBulkInvitations_happyPath_createsInvitations() {
        com.InvitationSystem.InvitationSystem.entity.Guest guest1 = com.InvitationSystem.InvitationSystem.entity.Guest.builder()
                .id(guestId)
                .eventId(eventId)
                .fullName("Guest One")
                .email("g1@example.com")
                .phone("+1111111111")
                .build();

        UUID guestId2 = UUID.randomUUID();
        com.InvitationSystem.InvitationSystem.entity.Guest guest2 = com.InvitationSystem.InvitationSystem.entity.Guest.builder()
                .id(guestId2)
                .eventId(eventId)
                .fullName("Guest Two")
                .email("g2@example.com")
                .phone("+2222222222")
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(guestRepository.findByEventId(eventId)).thenReturn(List.of(guest1, guest2));
        when(invitationRepository.findByEventIdAndGuestId(eq(eventId), any(UUID.class))).thenReturn(Optional.empty());
        when(tokenGeneratorService.generateSecureToken()).thenReturn("secure-bulk-token-1", "secure-bulk-token-2");
        when(qrCodeService.generateQRCodeImage(anyString())).thenReturn("qr-base64");
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(i -> {
            Invitation inv = i.getArgument(0);
            if (inv.getId() == null) inv.setId(UUID.randomUUID());
            return inv;
        });

        com.InvitationSystem.InvitationSystem.Dto.invitationsDto.BulkGenerationRequestDto bulkReq =
                com.InvitationSystem.InvitationSystem.Dto.invitationsDto.BulkGenerationRequestDto.builder()
                        .eventId(eventId)
                        .templateId(templateId)
                        .regenerationPolicy(com.InvitationSystem.InvitationSystem.Dto.invitationsDto.RegenerationPolicy.SKIP_EXISTING)
                        .build();

        com.InvitationSystem.InvitationSystem.Dto.invitationsDto.BulkGenerationResultDto result = invitationService.generateBulkInvitations(bulkReq);

        assertNotNull(result);
        assertEquals(2, result.getTotalGuests());
        assertEquals(2, result.getSuccessCount());
        assertEquals(0, result.getSkippedCount());
        assertEquals(0, result.getFailedCount());
        assertEquals(2, result.getSuccessfulInvitationIds().size());
    }

    @Test
    void generateBulkInvitations_skipExisting_skipsAlreadyGenerated() {
        com.InvitationSystem.InvitationSystem.entity.Guest guest1 = com.InvitationSystem.InvitationSystem.entity.Guest.builder()
                .id(guestId)
                .eventId(eventId)
                .fullName("Guest One")
                .email("g1@example.com")
                .build();

        Invitation existing = Invitation.builder()
                .id(invitationId)
                .eventId(eventId)
                .guestId(guestId)
                .status(InvitationStatus.GENERATED)
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(guestRepository.findByEventId(eventId)).thenReturn(List.of(guest1));
        when(invitationRepository.findByEventIdAndGuestId(eventId, guestId)).thenReturn(Optional.of(existing));

        com.InvitationSystem.InvitationSystem.Dto.invitationsDto.BulkGenerationRequestDto bulkReq =
                com.InvitationSystem.InvitationSystem.Dto.invitationsDto.BulkGenerationRequestDto.builder()
                        .eventId(eventId)
                        .templateId(templateId)
                        .regenerationPolicy(com.InvitationSystem.InvitationSystem.Dto.invitationsDto.RegenerationPolicy.SKIP_EXISTING)
                        .build();

        com.InvitationSystem.InvitationSystem.Dto.invitationsDto.BulkGenerationResultDto result = invitationService.generateBulkInvitations(bulkReq);

        assertNotNull(result);
        assertEquals(1, result.getTotalGuests());
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getSkippedCount());
        assertEquals(0, result.getFailedCount());
    }

    @Test
    void generateBulkInvitations_regenerateExisting_updatesExistingInvitation() {
        com.InvitationSystem.InvitationSystem.entity.Guest guest1 = com.InvitationSystem.InvitationSystem.entity.Guest.builder()
                .id(guestId)
                .eventId(eventId)
                .fullName("Guest One")
                .email("g1@example.com")
                .build();

        Invitation existing = Invitation.builder()
                .id(invitationId)
                .eventId(eventId)
                .guestId(guestId)
                .templateId(UUID.randomUUID())
                .status(InvitationStatus.GENERATED)
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(guestRepository.findByEventId(eventId)).thenReturn(List.of(guest1));
        when(invitationRepository.findByEventIdAndGuestId(eventId, guestId)).thenReturn(Optional.of(existing));
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(i -> i.getArgument(0));

        com.InvitationSystem.InvitationSystem.Dto.invitationsDto.BulkGenerationRequestDto bulkReq =
                com.InvitationSystem.InvitationSystem.Dto.invitationsDto.BulkGenerationRequestDto.builder()
                        .eventId(eventId)
                        .templateId(templateId)
                        .regenerationPolicy(com.InvitationSystem.InvitationSystem.Dto.invitationsDto.RegenerationPolicy.REGENERATE_EXISTING)
                        .build();

        com.InvitationSystem.InvitationSystem.Dto.invitationsDto.BulkGenerationResultDto result = invitationService.generateBulkInvitations(bulkReq);

        assertNotNull(result);
        assertEquals(1, result.getTotalGuests());
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getSkippedCount());
        assertEquals(0, result.getFailedCount());
        assertEquals(invitationId, result.getSuccessfulInvitationIds().get(0));
    }

    @Test
    void generateBulkInvitations_invalidGuestContact_recordsPartialFailure() {
        com.InvitationSystem.InvitationSystem.entity.Guest badGuest = com.InvitationSystem.InvitationSystem.entity.Guest.builder()
                .id(guestId)
                .eventId(eventId)
                .fullName("Bad Contact Guest")
                .email("  ")
                .phone("")
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(guestRepository.findByEventId(eventId)).thenReturn(List.of(badGuest));

        com.InvitationSystem.InvitationSystem.Dto.invitationsDto.BulkGenerationRequestDto bulkReq =
                com.InvitationSystem.InvitationSystem.Dto.invitationsDto.BulkGenerationRequestDto.builder()
                        .eventId(eventId)
                        .templateId(templateId)
                        .build();

        com.InvitationSystem.InvitationSystem.Dto.invitationsDto.BulkGenerationResultDto result = invitationService.generateBulkInvitations(bulkReq);

        assertNotNull(result);
        assertEquals(1, result.getTotalGuests());
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailedCount());
        assertEquals(1, result.getErrors().size());
        assertEquals(guestId, result.getErrors().get(0).getGuestId());
        assertTrue(result.getErrors().get(0).getErrorMessage().contains("valid contact info"));
    }
}
