package com.InvitationSystem.InvitationSystem.service.impl;

import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationDetailedResponseDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationResponseDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationScanResponseDto;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    private QRCodeService qrCodeService;
    @Mock
    private EmailService emailService;
    @Mock
    private TemplateProcessorService templateProcessorService;
    @Mock
    private PDFService pdfService;

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
    }

    @Test
    void createInvitation_happyPath_updatesDeliveryAndSentAt() {
        when(invitationRepository.findByEventIdAndGuestId(eventId, guestId)).thenReturn(Optional.empty());
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(qrCodeService.generateQRCodeImage(anyString())).thenReturn("qr-base64");
        when(templateProcessorService.renderTemplate(anyString(), any())).thenReturn("<html>rendered</html>");
        when(pdfService.generateInvitationCardPdf("<html>rendered</html>")).thenReturn("pdf-base64");
        when(invitationRepository.existsByUniqueToken(anyString())).thenReturn(false);
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
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> invitationService.createInvitation(request));
    }

    @Test
    void createInvitation_missingTemplateContent_throws() {
        template.setContent("   ");
        when(invitationRepository.findByEventIdAndGuestId(eventId, guestId)).thenReturn(Optional.empty());
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(qrCodeService.generateQRCodeImage(anyString())).thenReturn("qr-base64");
        when(invitationRepository.existsByUniqueToken(anyString())).thenReturn(false);
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(IllegalArgumentException.class, () -> invitationService.createInvitation(request));
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
        when(invitationRepository.existsByUniqueToken(anyString())).thenReturn(false);
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
    void scanInvitation_firstTime_marksUsedAndScanned() {
        Invitation invitation = Invitation.builder()
                .id(invitationId)
                .uniqueToken("tok")
                .status(InvitationStatus.SENT)
                .used(false)
                .scanned(false)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();
        when(invitationRepository.findByUniqueToken("tok")).thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InvitationScanResponseDto response = invitationService.scanInvitationByToken("tok");

        assertEquals(InvitationStatus.USED, response.getStatus());
        assertTrue(response.isScanned());
        assertEquals("Check-in successful", response.getMessage());
    }

    @Test
    void scanInvitation_alreadyUsed_returnsAlreadyCheckedIn() {
        Invitation invitation = Invitation.builder()
                .id(invitationId)
                .uniqueToken("tok")
                .status(InvitationStatus.USED)
                .used(true)
                .scanned(true)
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
        when(invitationRepository.existsByUniqueToken(anyString())).thenReturn(false);
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
                        .recipientEmail("guest@example.com")
                        .recipientPhone("+1234567890")
                        .expiresAt(LocalDateTime.now().plusDays(1))
                        .build()));
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InvitationScanResponseDto scan = invitationService.scanInvitationByToken(created.getUniqueToken());

        assertEquals(InvitationStatus.SENT, created.getStatus());
        assertEquals(DeliveryStatus.SENT_EMAIL, created.getDeliveryStatus());
        assertEquals("Check-in successful", scan.getMessage());
    }
}
