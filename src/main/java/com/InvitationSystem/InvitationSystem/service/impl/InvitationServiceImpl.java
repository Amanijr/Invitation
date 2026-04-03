package com.InvitationSystem.InvitationSystem.service.impl;

import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationResponseDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationDetailedResponseDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationScanResponseDto;
import com.InvitationSystem.InvitationSystem.entity.DeliveryStatus;
import com.InvitationSystem.InvitationSystem.entity.Event;
import com.InvitationSystem.InvitationSystem.entity.Invitation;
import com.InvitationSystem.InvitationSystem.entity.InvitationStatus;
import com.InvitationSystem.InvitationSystem.entity.Template;
import com.InvitationSystem.InvitationSystem.repository.EventRepository;
import com.InvitationSystem.InvitationSystem.repository.InvitationRepository;
import com.InvitationSystem.InvitationSystem.repository.TemplateRepository;
import com.InvitationSystem.InvitationSystem.service.InvitationService;
import com.InvitationSystem.InvitationSystem.util.QRCodeService;
import com.InvitationSystem.InvitationSystem.util.EmailService;
import com.InvitationSystem.InvitationSystem.util.PDFService;
import com.InvitationSystem.InvitationSystem.util.TemplateProcessorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class InvitationServiceImpl implements InvitationService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+0-9()\\-\\s]{7,20}$");

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private QRCodeService qrCodeService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private TemplateProcessorService templateProcessorService;

    @Autowired
    private PDFService pdfService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Override
    public InvitationResponseDto createInvitation(InvitationRequestDto request) {
        validateCreateRequest(request);

        invitationRepository.findByEventIdAndGuestId(request.getEventId(), request.getGuestId())
                .ifPresent(inv -> {
                    throw new IllegalArgumentException("Invitation already exists for this guest in this event");
                });

        Template template = templateRepository.findById(request.getTemplateId())
            .orElseThrow(() -> new IllegalArgumentException("Template not found with ID: " + request.getTemplateId()));

        Event event = eventRepository.findById(request.getEventId())
            .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + request.getEventId()));

        String uniqueToken = generateUniqueToken();
        String scanUrl = baseUrl + "/api/v1/invitations/scan/" + uniqueToken;
        String qrCodeBase64 = qrCodeService.generateQRCodeImage(scanUrl);
        LocalDateTime expiresAt = request.getExpiryDate();

        Invitation invitation = Invitation.builder()
                .eventId(request.getEventId())
                .templateId(request.getTemplateId())
                .guestId(request.getGuestId())
                .recipientPhone(request.getRecipientPhone())
                .recipientEmail(request.getRecipientEmail())
            .uniqueToken(uniqueToken)
            .qrCodeUrl(scanUrl)
            .qrCode(qrCodeBase64)
                .status(InvitationStatus.GENERATED)
                .deliveryStatus(DeliveryStatus.PENDING)
                .expiryDate(request.getExpiryDate())
            .expiresAt(expiresAt)
                .used(false)
            .scanned(false)
                .build();

        Invitation savedInvitation = invitationRepository.save(invitation);

        String guestName = (request.getGuestName() == null || request.getGuestName().isBlank())
            ? request.getRecipientEmail()
            : request.getGuestName();

        String eventDate = event.getEventDate() == null
            ? "TBD"
            : event.getEventDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("guestName", guestName);
        placeholders.put("eventName", event.getEventName());
        placeholders.put("eventDate", eventDate);
        placeholders.put("invitationUrl", baseUrl + "/api/v1/invitations/token/" + uniqueToken);
        placeholders.put("qrCode", "<img src=\"data:image/png;base64," + qrCodeBase64 + "\" width=\"200\" alt=\"QR Code\" />");

        String templateContent = template.getContent();
        if (templateContent == null || templateContent.isBlank()) {
            throw new IllegalArgumentException("Template content cannot be empty for invitation rendering");
        }

        String renderedHtml = templateProcessorService.renderTemplate(templateContent, placeholders);

        boolean attachPdf = request.getAttachPdf() == null || request.getAttachPdf();
        String invitationPdfBase64 = attachPdf ? pdfService.generateInvitationCardPdf(renderedHtml) : null;

        try {
            emailService.sendInvitationEmail(
                savedInvitation.getRecipientEmail(),
            "You're Invited: " + event.getEventName(),
            renderedHtml,
            invitationPdfBase64,
            "invitation-card-" + savedInvitation.getId() + ".pdf"
            );

            savedInvitation.setStatus(InvitationStatus.SENT);
            savedInvitation.setDeliveryStatus(DeliveryStatus.SENT_EMAIL);
            savedInvitation.setSentAt(LocalDateTime.now());
        } catch (Exception e) {
            savedInvitation.setStatus(InvitationStatus.FAILED);
            savedInvitation.setDeliveryStatus(DeliveryStatus.FAILED);
            invitationRepository.save(savedInvitation);
            throw new RuntimeException("Invitation email sending failed: " + e.getMessage(), e);
        }

        savedInvitation = invitationRepository.save(savedInvitation);
        
        return mapToResponseDto(savedInvitation);
    }

    @Override
    public InvitationDetailedResponseDto getInvitationById(UUID invitationId) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found with ID: " + invitationId));
        return mapToDetailedResponseDto(invitation);
    }

    @Override
    public InvitationDetailedResponseDto getInvitationByToken(String token) {
        Invitation invitation = invitationRepository.findByUniqueToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired invitation token"));
        return mapToDetailedResponseDto(invitation);
    }

    @Override
    public boolean validateInvitation(String token, String recipientPhone, String recipientEmail) {
        Invitation invitation = invitationRepository.findByUniqueToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid invitation token"));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = invitation.getExpiresAt() != null ? invitation.getExpiresAt() : invitation.getExpiryDate();
        if (expiresAt != null && now.isAfter(expiresAt)) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new IllegalArgumentException("Invitation expired");
        }

        if (invitation.isUsed() || invitation.isScanned() || invitation.getStatus() == InvitationStatus.USED) {
            throw new IllegalArgumentException("Invitation already used");
        }

        if ((recipientPhone != null && !recipientPhone.equals(invitation.getRecipientPhone())) ||
            (recipientEmail != null && !recipientEmail.equals(invitation.getRecipientEmail()))) {
            throw new IllegalArgumentException("This invitation is not shareable and is not tied to your phone/email");
        }

        return true;
    }

    @Override
    public List<InvitationDetailedResponseDto> getInvitationsByEvent(UUID eventId) {
        return invitationRepository.findByEventId(eventId).stream()
                .map(this::mapToDetailedResponseDto)
                .toList();
    }

    @Override
    public List<InvitationDetailedResponseDto> getInvitationsByGuest(UUID guestId) {
        return invitationRepository.findByGuestId(guestId).stream()
                .map(this::mapToDetailedResponseDto)
                .toList();
    }

    @Override
    public List<InvitationResponseDto> getAllInvitations() {
        return invitationRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public List<InvitationDetailedResponseDto> getInvitationsByStatus(InvitationStatus status) {
        return invitationRepository.findByStatus(status).stream()
                .map(this::mapToDetailedResponseDto)
                .toList();
    }

    @Override
    public InvitationDetailedResponseDto markAsSent(UUID invitationId) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found with ID: " + invitationId));

        invitation.setStatus(InvitationStatus.SENT);
        invitation.setDeliveryStatus(DeliveryStatus.SENT_EMAIL);
        invitation.setSentAt(LocalDateTime.now());

        Invitation updatedInvitation = invitationRepository.save(invitation);
        return mapToDetailedResponseDto(updatedInvitation);
    }

    @Override
    public InvitationDetailedResponseDto markAsDelivered(UUID invitationId) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found with ID: " + invitationId));

        invitation.setStatus(InvitationStatus.DELIVERED);
        invitation.setDeliveredAt(LocalDateTime.now());

        Invitation updatedInvitation = invitationRepository.save(invitation);
        return mapToDetailedResponseDto(updatedInvitation);
    }

    @Override
    public InvitationDetailedResponseDto markAsOpened(UUID invitationId) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found with ID: " + invitationId));

        invitation.setStatus(InvitationStatus.OPENED);
        invitation.setOpenedAt(LocalDateTime.now());

        Invitation updatedInvitation = invitationRepository.save(invitation);
        return mapToDetailedResponseDto(updatedInvitation);
    }

    @Override
    public InvitationDetailedResponseDto markAsUsed(UUID invitationId) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found with ID: " + invitationId));

        invitation.setStatus(InvitationStatus.USED);
        invitation.setUsed(true);  // fixed

        Invitation updatedInvitation = invitationRepository.save(invitation);
        return mapToDetailedResponseDto(updatedInvitation);
    }

    @Override
    public InvitationDetailedResponseDto generateQrCode(UUID invitationId, String qrCodeUrl) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found with ID: " + invitationId));

        // If no URL provided, generate QR code from token
        if (qrCodeUrl == null || qrCodeUrl.isEmpty()) {
            qrCodeUrl = baseUrl + "/api/v1/invitations/scan/" + invitation.getUniqueToken();
        }

        invitation.setQrCodeUrl(qrCodeUrl);
        invitation.setQrCode(qrCodeService.generateQRCodeImage(qrCodeUrl));
        Invitation updatedInvitation = invitationRepository.save(invitation);
        return mapToDetailedResponseDto(updatedInvitation);
    }

    @Override
    public InvitationScanResponseDto scanInvitationByToken(String token) {
        Invitation invitation = invitationRepository.findByUniqueToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid QR Code"));

        if (invitation.getStatus() == InvitationStatus.USED || invitation.isScanned()) {
            return new InvitationScanResponseDto(
                    invitation.getId(),
                    invitation.getUniqueToken(),
                    invitation.getStatus(),
                    invitation.isScanned(),
                    invitation.getScannedAt(),
                    "Already checked in"
            );
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = invitation.getExpiresAt() != null ? invitation.getExpiresAt() : invitation.getExpiryDate();

        if (expiresAt != null && now.isAfter(expiresAt)) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            Invitation expiredInvitation = invitationRepository.save(invitation);
            return new InvitationScanResponseDto(
                    expiredInvitation.getId(),
                    expiredInvitation.getUniqueToken(),
                    expiredInvitation.getStatus(),
                    expiredInvitation.isScanned(),
                    expiredInvitation.getScannedAt(),
                    "Invitation expired"
            );
        }

        invitation.setStatus(InvitationStatus.USED);
        invitation.setUsed(true);
        invitation.setScanned(true);
        invitation.setScannedAt(now);

        Invitation updatedInvitation = invitationRepository.save(invitation);
        return new InvitationScanResponseDto(
                updatedInvitation.getId(),
                updatedInvitation.getUniqueToken(),
                updatedInvitation.getStatus(),
                updatedInvitation.isScanned(),
                updatedInvitation.getScannedAt(),
                "Check-in successful"
        );
    }

    @Override
    public void deleteInvitation(UUID invitationId) {
        invitationRepository.deleteById(invitationId);
    }

    @Override
    public List<InvitationDetailedResponseDto> getInvitationsByBulkUpload(UUID bulkUploadSessionId) {
        return invitationRepository.findByBulkUploadSessionId(bulkUploadSessionId).stream()
                .map(this::mapToDetailedResponseDto)
                .toList();
    }

    private InvitationResponseDto mapToResponseDto(Invitation invitation) {
        InvitationResponseDto dto = new InvitationResponseDto();
        dto.setId(invitation.getId());
        dto.setEventId(invitation.getEventId());
        dto.setTemplateId(invitation.getTemplateId());
        dto.setGuestId(invitation.getGuestId());
        dto.setRecipientPhone(invitation.getRecipientPhone());
        dto.setRecipientEmail(invitation.getRecipientEmail());
        dto.setUniqueToken(invitation.getUniqueToken());
        dto.setQrCodeUrl(invitation.getQrCodeUrl());
        dto.setQrCode(invitation.getQrCode());
        dto.setUsed(invitation.isUsed());
        dto.setScanned(invitation.isScanned());
        dto.setStatus(invitation.getStatus());
        dto.setDeliveryStatus(invitation.getDeliveryStatus());
        dto.setGeneratedAt(invitation.getGeneratedAt());
        dto.setExpiryDate(invitation.getExpiryDate());
        dto.setExpiresAt(invitation.getExpiresAt());
        dto.setScannedAt(invitation.getScannedAt());
        return dto;
    }

    private InvitationDetailedResponseDto mapToDetailedResponseDto(Invitation invitation) {
        InvitationDetailedResponseDto dto = new InvitationDetailedResponseDto();
        dto.setId(invitation.getId());
        dto.setEventId(invitation.getEventId());
        dto.setTemplateId(invitation.getTemplateId());
        dto.setGuestId(invitation.getGuestId());
        dto.setRecipientPhone(invitation.getRecipientPhone());
        dto.setRecipientEmail(invitation.getRecipientEmail());
        dto.setUniqueToken(invitation.getUniqueToken());
        dto.setQrCodeUrl(invitation.getQrCodeUrl());
        dto.setQrCode(invitation.getQrCode());
        dto.setUsed(invitation.isUsed());
        dto.setScanned(invitation.isScanned());
        dto.setStatus(invitation.getStatus());
        dto.setDeliveryStatus(invitation.getDeliveryStatus());
        dto.setSentAt(invitation.getSentAt());
        dto.setDeliveredAt(invitation.getDeliveredAt());
        dto.setOpenedAt(invitation.getOpenedAt());
        dto.setGeneratedAt(invitation.getGeneratedAt());
        dto.setExpiryDate(invitation.getExpiryDate());
        dto.setExpiresAt(invitation.getExpiresAt());
        dto.setScannedAt(invitation.getScannedAt());
        dto.setBulkUploadSessionId(invitation.getBulkUploadSessionId());
        return dto;
    }

    private String generateUniqueToken() {
        String token;
        do {
            token = UUID.randomUUID().toString();
        } while (invitationRepository.existsByUniqueToken(token));
        return token;
    }

    private void validateCreateRequest(InvitationRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Invitation request is required");
        }
        if (request.getEventId() == null) {
            throw new IllegalArgumentException("eventId is required");
        }
        if (request.getGuestId() == null) {
            throw new IllegalArgumentException("guestId is required");
        }
        if (request.getTemplateId() == null) {
            throw new IllegalArgumentException("templateId is required");
        }

        String recipientEmail = request.getRecipientEmail();
        String recipientPhone = request.getRecipientPhone();
        boolean hasEmail = recipientEmail != null && !recipientEmail.isBlank();
        boolean hasPhone = recipientPhone != null && !recipientPhone.isBlank();

        if (!hasEmail && !hasPhone) {
            throw new IllegalArgumentException("At least one guest contact (email or phone) is required");
        }

        if (hasEmail && !EMAIL_PATTERN.matcher(recipientEmail).matches()) {
            throw new IllegalArgumentException("Invalid recipient email format");
        }

        if (hasPhone && !PHONE_PATTERN.matcher(recipientPhone).matches()) {
            throw new IllegalArgumentException("Invalid recipient phone format");
        }

        if (!hasEmail) {
            throw new IllegalArgumentException("recipientEmail is required for email delivery");
        }
    }
}
