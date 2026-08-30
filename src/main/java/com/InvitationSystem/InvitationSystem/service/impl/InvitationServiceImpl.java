package com.InvitationSystem.InvitationSystem.service.impl;

import com.InvitationSystem.InvitationSystem.Dto.checkInDto.CheckInHistoryDto;
import com.InvitationSystem.InvitationSystem.Dto.checkInDto.CheckInRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.checkInDto.CheckInResponseDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationResponseDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationDetailedResponseDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationScanResponseDto;
import com.InvitationSystem.InvitationSystem.entity.*;
import com.InvitationSystem.InvitationSystem.repository.CheckInRepository;
import com.InvitationSystem.InvitationSystem.repository.EventRepository;
import com.InvitationSystem.InvitationSystem.repository.GuestRepository;
import com.InvitationSystem.InvitationSystem.repository.InvitationRepository;
import com.InvitationSystem.InvitationSystem.repository.TemplateRepository;
import com.InvitationSystem.InvitationSystem.service.CheckInAuditRecorder;
import com.InvitationSystem.InvitationSystem.service.InvitationService;
import com.InvitationSystem.InvitationSystem.security.EventAuthorization;
import com.InvitationSystem.InvitationSystem.util.QRCodeService;
import com.InvitationSystem.InvitationSystem.util.EmailService;
import com.InvitationSystem.InvitationSystem.util.GuestCardLinks;
import com.InvitationSystem.InvitationSystem.util.PDFService;
import com.InvitationSystem.InvitationSystem.util.TemplateAvailability;
import com.InvitationSystem.InvitationSystem.util.TemplateProcessorService;
import com.InvitationSystem.InvitationSystem.util.TokenGeneratorService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    private GuestRepository guestRepository;

    @Autowired
    private CheckInRepository checkInRepository;

    @Autowired
    private CheckInAuditRecorder checkInAuditRecorder;

    @Autowired
    private TokenGeneratorService tokenGeneratorService;

    @Autowired
    private QRCodeService qrCodeService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private TemplateProcessorService templateProcessorService;

    @Autowired
    private PDFService pdfService;

    @Autowired
    private com.InvitationSystem.InvitationSystem.service.ECardRenderingEngineService eCardRenderingEngineService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.public-url:http://localhost:5173}")
    private String publicUrl;

    @Override
    @Transactional
    public InvitationResponseDto createInvitation(InvitationRequestDto request) {
        validateCreateRequest(request);

        invitationRepository.findByEventIdAndGuestId(request.getEventId(), request.getGuestId())
                .ifPresent(inv -> {
                    throw new IllegalArgumentException("Invitation already exists for this guest in this event");
                });

        Event event = eventRepository.findById(request.getEventId())
            .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + request.getEventId()));

        UUID templateId = request.getTemplateId() != null ? request.getTemplateId() : event.getCurrentTemplateId();
        if (templateId == null) {
            throw new IllegalArgumentException("This event has no current invitation template. Assign a template first.");
        }

        Template template = templateRepository.findById(templateId)
            .orElseThrow(() -> new IllegalArgumentException("Template not found with ID: " + templateId));
        if (!TemplateAvailability.isAvailableForEvent(template, event.getId())) {
            throw new IllegalArgumentException("Template is not available for this event");
        }

        AdmissionType admissionType = AdmissionType.fromNullable(request.getAdmissionType());
        String uniqueToken = generateUniqueToken();
        String scanUrl = baseUrl + "/api/v1/invitations/scan/" + uniqueToken;
        String qrCodeBase64 = qrCodeService.generateQRCodeImage(scanUrl);
        LocalDateTime expiresAt = request.getExpiryDate();

        Invitation invitation = Invitation.builder()
                .eventId(request.getEventId())
                .templateId(template.getId())
                .templateVersion(template.resolvedVersion())
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
                .rsvpStatus(RsvpStatus.NO_REPLY)
                .partySize(admissionType.getAdmissionLimit())
                .admissionType(admissionType)
                .admissionLimit(admissionType.getAdmissionLimit())
                .usedAdmissions(0)
                .revoked(false)
                .build();

        Invitation savedInvitation = invitationRepository.save(invitation);

        // Render personalized graphic E-Card and store reference
        try {
            savedInvitation = eCardRenderingEngineService.renderAndStoreCard(savedInvitation);
        } catch (Exception e) {
            System.err.println("Warning: Graphic card rendering failed: " + e.getMessage());
        }

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
        placeholders.put("invitationUrl", GuestCardLinks.cardViewUrl(publicUrl, uniqueToken));
        placeholders.put("qrCode", "<img src=\"data:image/png;base64," + qrCodeBase64 + "\" width=\"200\" alt=\"QR Code\" />");

        String templateContent = template.getContent();
        String renderedHtml = (templateContent != null && !templateContent.isBlank())
                ? templateProcessorService.renderTemplate(templateContent, placeholders)
                : "<div style='text-align:center;'><h2>Invitation for " + guestName + "</h2><p>" + event.getEventName() + "</p></div>";

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
    public List<InvitationResponseDto> getInvitationsForActor(UUID actorId, UserRole role) {
        if (role == UserRole.ADMIN) {
            return getAllInvitations();
        }
        List<UUID> eventIds = eventRepository.findByCreatedBy(actorId).stream()
                .map(Event::getId)
                .toList();
        if (eventIds.isEmpty()) {
            return List.of();
        }
        return invitationRepository.findByEventIdIn(eventIds).stream()
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
        invitation.setUsed(true);
        invitation.setUsedAt(LocalDateTime.now());

        Invitation updatedInvitation = invitationRepository.save(invitation);
        return mapToDetailedResponseDto(updatedInvitation);
    }

    @Override
    public InvitationDetailedResponseDto generateQrCode(UUID invitationId, String qrCodeUrl) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found with ID: " + invitationId));

        if (qrCodeUrl == null || qrCodeUrl.isEmpty()) {
            qrCodeUrl = baseUrl + "/api/v1/invitations/scan/" + invitation.getUniqueToken();
        }

        invitation.setQrCodeUrl(qrCodeUrl);
        invitation.setQrCode(qrCodeService.generateQRCodeImage(qrCodeUrl));
        Invitation updatedInvitation = invitationRepository.save(invitation);
        return mapToDetailedResponseDto(updatedInvitation);
    }

    @Override
    @Transactional(readOnly = true)
    public InvitationScanResponseDto scanInvitationByToken(String token) {
        Invitation invitation = invitationRepository.findByUniqueToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid QR Code"));

        return new InvitationScanResponseDto(
                invitation.getId(),
                token,
                invitation.getStatus(),
                invitation.isScanned(),
                invitation.getScannedAt(),
                previewMessage(invitation)
        );
    }

    private String previewMessage(Invitation invitation) {
        if (invitation.isRevoked() || invitation.getStatus() == InvitationStatus.REVOKED) {
            return "Invitation has been revoked";
        }
        if (invitation.getStatus() == InvitationStatus.USED
                || invitation.resolvedUsedAdmissions() >= invitation.resolvedAdmissionLimit()) {
            return "Already checked in";
        }
        return "Invitation valid";
    }

    @Override
    @Transactional
    public CheckInResponseDto verifyInvitation(CheckInRequestDto request) {
        if (request == null || request.getToken() == null || request.getToken().isBlank()) {
            CheckIn audit = checkInAuditRecorder.record(CheckIn.builder()
                    .scannedAt(LocalDateTime.now())
                    .scannerId(request != null ? request.getScannerId() : null)
                    .eventId(request != null ? request.getEventId() : null)
                    .result(CheckInResult.INVALID_TOKEN)
                    .notes("Missing verification token")
                    .build());
            return CheckInResponseDto.builder()
                    .checkInId(audit != null ? audit.getId() : null)
                    .result(CheckInResult.INVALID_TOKEN)
                    .message("Invalid verification token")
                    .scannedAt(audit != null && audit.getScannedAt() != null ? audit.getScannedAt() : LocalDateTime.now())
                    .scannerId(request != null ? request.getScannerId() : null)
                    .build();
        }

        if (request.getEventId() == null) {
            CheckIn audit = checkInAuditRecorder.record(CheckIn.builder()
                    .scannedAt(LocalDateTime.now())
                    .scannerId(request.getScannerId())
                    .result(CheckInResult.EVENT_MISMATCH)
                    .notes("Missing eventId")
                    .build());
            return CheckInResponseDto.builder()
                    .checkInId(audit != null ? audit.getId() : null)
                    .token(request.getToken())
                    .result(CheckInResult.EVENT_MISMATCH)
                    .message("eventId is required")
                    .scannedAt(audit != null && audit.getScannedAt() != null ? audit.getScannedAt() : LocalDateTime.now())
                    .scannerId(request.getScannerId())
                    .belongsToScannedEvent(false)
                    .build();
        }

        LocalDateTime now = LocalDateTime.now();

        // 1. Atomic pessimistic write lock lookup by token
        Optional<Invitation> optionalInvitation = invitationRepository.findByUniqueTokenForUpdate(request.getToken());

        if (optionalInvitation.isEmpty()) {
            CheckIn audit = checkInAuditRecorder.record(CheckIn.builder()
                    .scannedAt(now)
                    .scannerId(request.getScannerId())
                    .eventId(request.getEventId())
                    .result(CheckInResult.INVALID_TOKEN)
                    .notes("Token does not exist")
                    .build());
            return CheckInResponseDto.builder()
                    .checkInId(audit != null ? audit.getId() : null)
                    .token(request.getToken())
                    .result(CheckInResult.INVALID_TOKEN)
                    .message("Invalid or non-existent token")
                    .scannedAt(now)
                    .scannerId(request.getScannerId())
                    .build();
        }

        Invitation invitation = optionalInvitation.get();

        // Event scope is required; mismatch rejects without consuming an admission.
        if (request.getEventId() != null && !request.getEventId().equals(invitation.getEventId())) {
            CheckIn audit = checkInAuditRecorder.record(CheckIn.builder()
                    .invitationId(invitation.getId())
                    .eventId(request.getEventId())
                    .scannedAt(now)
                    .scannerId(request.getScannerId())
                    .result(CheckInResult.EVENT_MISMATCH)
                    .notes("Invitation does not belong to specified event")
                    .build());
            return CheckInResponseDto.builder()
                    .checkInId(audit != null ? audit.getId() : null)
                    .invitationId(invitation.getId())
                    .eventId(request.getEventId())
                    .token(request.getToken())
                    .result(CheckInResult.EVENT_MISMATCH)
                    .message("Invitation does not belong to this event")
                    .guestName(guestNameOf(invitation))
                    .eventName(eventNameOf(invitation.getEventId()))
                    .admissionType(AdmissionType.fromNullable(invitation.getAdmissionType()))
                    .admissionLimit(invitation.resolvedAdmissionLimit())
                    .usedAdmissions(invitation.resolvedUsedAdmissions())
                    .remainingAdmissions(invitation.remainingAdmissions())
                    .revoked(invitation.isRevoked())
                    .belongsToScannedEvent(false)
                    .entitlementState(CheckInEntitlementState.INVALID)
                    .scannedAt(now)
                    .scannerId(request.getScannerId())
                    .build();
        }

        if (invitation.isRevoked() || invitation.getStatus() == InvitationStatus.REVOKED) {
            CheckIn audit = checkInAuditRecorder.record(CheckIn.builder()
                    .invitationId(invitation.getId())
                    .eventId(invitation.getEventId())
                    .scannedAt(now)
                    .scannerId(request.getScannerId())
                    .result(CheckInResult.REVOKED)
                    .notes("Invitation revoked")
                    .build());
            return checkInResponse(invitation, request, audit, CheckInResult.REVOKED,
                    "Invitation has been revoked", now, CheckInEntitlementState.REVOKED);
        }

        // 4. Expiration check
        LocalDateTime expiresAt = invitation.getExpiresAt() != null ? invitation.getExpiresAt() : invitation.getExpiryDate();
        if (invitation.getStatus() == InvitationStatus.EXPIRED || (expiresAt != null && now.isAfter(expiresAt))) {
            if (invitation.getStatus() != InvitationStatus.EXPIRED) {
                invitation.setStatus(InvitationStatus.EXPIRED);
                invitationRepository.save(invitation);
            }
            CheckIn audit = checkInAuditRecorder.record(CheckIn.builder()
                    .invitationId(invitation.getId())
                    .eventId(invitation.getEventId())
                    .scannedAt(now)
                    .scannerId(request.getScannerId())
                    .result(CheckInResult.EXPIRED)
                    .notes("Invitation expired")
                    .build());

            return checkInResponse(invitation, request, audit, CheckInResult.EXPIRED,
                    "Invitation has expired", now, CheckInEntitlementState.INVALID);
        }

        int usedCount = invitation.resolvedUsedAdmissions();
        int limit = invitation.resolvedAdmissionLimit();
        if (invitation.getStatus() == InvitationStatus.USED || usedCount >= limit) {
            CheckIn audit = checkInAuditRecorder.record(CheckIn.builder()
                    .invitationId(invitation.getId())
                    .eventId(invitation.getEventId())
                    .scannedAt(now)
                    .scannerId(request.getScannerId())
                    .result(CheckInResult.ALREADY_USED)
                    .notes("Admission limit reached")
                    .build());

            return checkInResponse(invitation, request, audit, CheckInResult.ALREADY_USED,
                    "Already checked in", now, CheckInEntitlementState.FULLY_USED);
        }

        int nextUsed = usedCount + 1;
        invitation.setUsedAdmissions(nextUsed);
        invitation.setScanned(true);
        invitation.setScannedAt(now);
        if (nextUsed >= limit) {
            invitation.setStatus(InvitationStatus.USED);
            invitation.setUsed(true);
            invitation.setUsedAt(now);
        } else {
            invitation.setUsed(false);
        }

        Invitation updatedInvitation = invitationRepository.save(invitation);

        CheckIn audit = checkInAuditRecorder.record(CheckIn.builder()
                .invitationId(updatedInvitation.getId())
                .eventId(updatedInvitation.getEventId())
                .scannedAt(now)
                .scannerId(request.getScannerId())
                .result(CheckInResult.SUCCESS)
                .notes("Check-in successful")
                .build());

        CheckInEntitlementState state = updatedInvitation.entitlementState();
        String successMessage = updatedInvitation.remainingAdmissions() == 0
                ? "Check-in successful"
                : "Check-in successful. " + updatedInvitation.remainingAdmissions() + " admission remaining.";

        return checkInResponse(updatedInvitation, request, audit, CheckInResult.SUCCESS,
                successMessage, now, state);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CheckInHistoryDto> getCheckInHistory(UUID eventId) {
        return mapCheckInHistory(eventId != null
                ? checkInRepository.findByEventId(eventId)
                : checkInRepository.findTop100ByOrderByScannedAtDesc());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CheckInHistoryDto> getCheckInHistory(UUID eventId, UUID actorId, UserRole actorRole) {
        if (eventId != null) {
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + eventId));
            EventAuthorization.requireEventOwnerOrAdmin(event, actorId, actorRole);
            return getCheckInHistory(eventId);
        }
        if (actorRole == UserRole.ADMIN) {
            return getCheckInHistory(null);
        }
        List<UUID> eventIds = eventRepository.findByCreatedBy(actorId).stream()
                .map(Event::getId)
                .toList();
        if (eventIds.isEmpty()) {
            return List.of();
        }
        List<CheckIn> rows = checkInRepository.findByEventIdInOrderByScannedAtDesc(eventIds);
        if (rows.size() > 100) {
            rows = rows.subList(0, 100);
        }
        return mapCheckInHistory(rows);
    }

    private List<CheckInHistoryDto> mapCheckInHistory(List<CheckIn> rows) {
        Set<UUID> invitationIds = rows.stream()
                .map(CheckIn::getInvitationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, Invitation> invitations = invitationIds.isEmpty()
                ? Map.of()
                : invitationRepository.findAllById(invitationIds).stream()
                        .collect(Collectors.toMap(Invitation::getId, invitation -> invitation));
        return rows.stream()
                .map(row -> {
                    Invitation invitation = row.getInvitationId() != null
                            ? invitations.get(row.getInvitationId())
                            : null;
                    return CheckInHistoryDto.builder()
                            .id(row.getId())
                            .invitationId(row.getInvitationId())
                            .eventId(row.getEventId())
                            .scannerId(row.getScannerId())
                            .token(invitation != null ? invitation.getUniqueToken() : null)
                            .result(row.getResult())
                            .notes(row.getNotes())
                            .scannedAt(row.getScannedAt())
                            .build();
                })
                .toList();
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

    @Override
    public com.InvitationSystem.InvitationSystem.Dto.invitationsDto.BulkGenerationResultDto generateBulkInvitations(
            com.InvitationSystem.InvitationSystem.Dto.invitationsDto.BulkGenerationRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Bulk generation request cannot be null");
        }
        if (request.getEventId() == null) {
            throw new IllegalArgumentException("eventId is required");
        }

        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + request.getEventId()));

        UUID templateId = request.getTemplateId() != null ? request.getTemplateId() : event.getCurrentTemplateId();
        if (templateId == null) {
            throw new IllegalArgumentException("This event has no current invitation template. Assign a template first.");
        }

        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found with ID: " + templateId));
        if (!TemplateAvailability.isAvailableForEvent(template, event.getId())) {
            throw new IllegalArgumentException("Template is not available for this event");
        }

        List<Guest> guests;
        if (request.getGuestIds() != null && !request.getGuestIds().isEmpty()) {
            guests = guestRepository.findAllById(request.getGuestIds()).stream()
                    .filter(g -> g.getEventId().equals(request.getEventId()))
                    .toList();
        } else {
            guests = guestRepository.findByEventId(request.getEventId());
        }

        com.InvitationSystem.InvitationSystem.Dto.invitationsDto.RegenerationPolicy policy =
                request.getRegenerationPolicy() != null ? request.getRegenerationPolicy() :
                        com.InvitationSystem.InvitationSystem.Dto.invitationsDto.RegenerationPolicy.SKIP_EXISTING;

        int total = guests.size();
        int successCount = 0;
        int skippedCount = 0;
        int failedCount = 0;
        List<UUID> successfulInvitationIds = new java.util.ArrayList<>();
        List<com.InvitationSystem.InvitationSystem.Dto.invitationsDto.BulkGenerationErrorDto> errorLogs = new java.util.ArrayList<>();

        for (Guest guest : guests) {
            try {
                if (!request.getEventId().equals(guest.getEventId())) {
                    throw new IllegalArgumentException("Guest ID " + guest.getId() + " does not belong to event ID " + request.getEventId());
                }

                String recipientEmail = guest.getEmail();
                String recipientPhone = guest.getPhone();
                boolean hasEmail = recipientEmail != null && !recipientEmail.isBlank();
                boolean hasPhone = recipientPhone != null && !recipientPhone.isBlank();

                if (!hasEmail && !hasPhone) {
                    throw new IllegalArgumentException("Guest has no valid contact info (email or phone)");
                }

                Optional<Invitation> existingOpt = invitationRepository.findByEventIdAndGuestId(request.getEventId(), guest.getId());

                if (existingOpt.isPresent()) {
                    Invitation existing = existingOpt.get();
                    if (policy == com.InvitationSystem.InvitationSystem.Dto.invitationsDto.RegenerationPolicy.SKIP_EXISTING && existing.getStatus() != InvitationStatus.FAILED) {
                        skippedCount++;
                        continue;
                    } else {
                        // REGENERATE_EXISTING or retry FAILED: update template, contact info, status and re-render
                        existing.setTemplateId(template.getId());
                        existing.setTemplateVersion(template.resolvedVersion());
                        existing.setRecipientPhone(recipientPhone);
                        existing.setRecipientEmail(recipientEmail);
                        if (existing.getStatus() == InvitationStatus.FAILED) {
                            existing.setStatus(InvitationStatus.GENERATED);
                            existing.setDeliveryStatus(DeliveryStatus.PENDING);
                        }
                        if (request.getExpiryDate() != null) {
                            existing.setExpiryDate(request.getExpiryDate());
                            existing.setExpiresAt(request.getExpiryDate());
                        }

                        // Re-render card
                        try {
                            existing = eCardRenderingEngineService.renderAndStoreCard(existing);
                        } catch (Exception e) {
                            System.err.println("Warning: Card rendering failed for guest " + guest.getId() + ": " + e.getMessage());
                        }
                        Invitation saved = invitationRepository.save(existing);

                        successfulInvitationIds.add(saved.getId());
                        successCount++;
                    }
                } else {
                    // New invitation generation
                    String uniqueToken = generateUniqueToken();
                    String scanUrl = baseUrl + "/api/v1/invitations/scan/" + uniqueToken;
                    String qrCodeBase64 = qrCodeService.generateQRCodeImage(scanUrl);
                    LocalDateTime expiresAt = request.getExpiryDate() != null ? request.getExpiryDate() : LocalDateTime.now().plusDays(30);

                    Invitation invitation = Invitation.builder()
                            .eventId(request.getEventId())
                            .templateId(template.getId())
                            .templateVersion(template.resolvedVersion())
                            .guestId(guest.getId())
                            .recipientPhone(recipientPhone)
                            .recipientEmail(recipientEmail)
                            .uniqueToken(uniqueToken)
                            .qrCodeUrl(scanUrl)
                            .qrCode(qrCodeBase64)
                            .status(InvitationStatus.GENERATED)
                            .deliveryStatus(DeliveryStatus.PENDING)
                            .expiryDate(expiresAt)
                            .expiresAt(expiresAt)
                            .used(false)
                            .scanned(false)
                            .rsvpStatus(RsvpStatus.NO_REPLY)
                            .partySize(AdmissionType.SINGLE.getAdmissionLimit())
                            .admissionType(AdmissionType.SINGLE)
                            .admissionLimit(AdmissionType.SINGLE.getAdmissionLimit())
                            .usedAdmissions(0)
                            .revoked(false)
                            .build();

                    Invitation savedInvitation = invitationRepository.save(invitation);

                    try {
                        savedInvitation = eCardRenderingEngineService.renderAndStoreCard(savedInvitation);
                    } catch (Exception e) {
                        System.err.println("Warning: Card rendering failed for guest " + guest.getId() + ": " + e.getMessage());
                    }

                    successfulInvitationIds.add(savedInvitation.getId());
                    successCount++;
                }
            } catch (Exception e) {
                failedCount++;
                errorLogs.add(com.InvitationSystem.InvitationSystem.Dto.invitationsDto.BulkGenerationErrorDto.builder()
                        .guestId(guest.getId())
                        .guestName(guest.getFullName() != null ? guest.getFullName() : "Unknown Guest")
                        .errorMessage(e.getMessage())
                        .build());
            }
        }

        return com.InvitationSystem.InvitationSystem.Dto.invitationsDto.BulkGenerationResultDto.builder()
                .eventId(request.getEventId())
                .templateId(template.getId())
                .totalGuests(total)
                .successCount(successCount)
                .skippedCount(skippedCount)
                .failedCount(failedCount)
                .successfulInvitationIds(successfulInvitationIds)
                .errors(errorLogs)
                .processedAt(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public InvitationResponseDto issueInheritedInvitation(UUID eventId, UUID guestId, AdmissionType admissionType) {
        Optional<Invitation> existing = invitationRepository.findByEventIdAndGuestId(eventId, guestId);
        if (existing.isPresent()) {
            return mapToResponseDto(existing.get());
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + eventId));
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new IllegalArgumentException("Guest not found with ID: " + guestId));
        if (!eventId.equals(guest.getEventId())) {
            throw new IllegalArgumentException("Guest does not belong to this event");
        }
        if (event.getCurrentTemplateId() == null) {
            throw new IllegalArgumentException("This event has no current invitation template. Assign a template first.");
        }

        Template template = templateRepository.findById(event.getCurrentTemplateId())
                .orElseThrow(() -> new IllegalArgumentException("Template not found with ID: " + event.getCurrentTemplateId()));
        if (!TemplateAvailability.isAvailableForEvent(template, event.getId())) {
            throw new IllegalArgumentException("Template is not available for this event");
        }

        try {
            Invitation saved = persistCredentialInvitation(event, guest, template, admissionType, LocalDateTime.now().plusDays(30));
            return mapToResponseDto(saved);
        } catch (DataIntegrityViolationException duplicate) {
            return invitationRepository.findByEventIdAndGuestId(eventId, guestId)
                    .map(this::mapToResponseDto)
                    .orElseThrow(() -> duplicate);
        }
    }

    @Override
    @Transactional
    public int regenerateInvitationsForTemplateChange(
            UUID eventId,
            UUID templateId,
            int templateVersion,
            TemplateChangeScope scope) {
        if (scope == null || scope == TemplateChangeScope.NEW_GUESTS_ONLY) {
            return 0;
        }
        List<Invitation> invitations = invitationRepository.findByEventId(eventId);
        int regenerated = 0;
        for (Invitation invitation : invitations) {
            if (invitation.isRevoked() || invitation.getStatus() == InvitationStatus.REVOKED) {
                continue;
            }
            if (scope == TemplateChangeScope.UNSENT_INVITATIONS && !isUnsent(invitation)) {
                continue;
            }
            invitation.setTemplateId(templateId);
            invitation.setTemplateVersion(templateVersion);
            try {
                invitation = eCardRenderingEngineService.renderAndStoreCard(invitation);
            } catch (Exception e) {
                System.err.println("Warning: Card rendering failed for invitation " + invitation.getId() + ": " + e.getMessage());
            }
            invitationRepository.save(invitation);
            regenerated++;
        }
        return regenerated;
    }

    @Override
    @Transactional
    public InvitationDetailedResponseDto revokeInvitation(UUID invitationId, UUID actorId, UserRole actorRole) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found with ID: " + invitationId));
        Event event = eventRepository.findById(invitation.getEventId())
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + invitation.getEventId()));
        EventAuthorization.requireEventOwnerOrAdmin(event, actorId, actorRole);
        invitation.setRevoked(true);
        invitation.setRevokedAt(LocalDateTime.now());
        invitation.setStatus(InvitationStatus.REVOKED);
        return mapToDetailedResponseDto(invitationRepository.save(invitation));
    }

    private Invitation persistCredentialInvitation(
            Event event,
            Guest guest,
            Template template,
            AdmissionType admissionType,
            LocalDateTime expiry) {
        AdmissionType type = AdmissionType.fromNullable(admissionType);
        String uniqueToken = generateUniqueToken();
        String scanUrl = baseUrl + "/api/v1/invitations/scan/" + uniqueToken;
        String qrCodeBase64 = qrCodeService.generateQRCodeImage(scanUrl);

        Invitation invitation = Invitation.builder()
                .eventId(event.getId())
                .templateId(template.getId())
                .templateVersion(template.resolvedVersion())
                .guestId(guest.getId())
                .recipientPhone(guest.getPhone())
                .recipientEmail(guest.getEmail())
                .uniqueToken(uniqueToken)
                .qrCodeUrl(scanUrl)
                .qrCode(qrCodeBase64)
                .status(InvitationStatus.GENERATED)
                .deliveryStatus(DeliveryStatus.PENDING)
                .expiryDate(expiry)
                .expiresAt(expiry)
                .used(false)
                .scanned(false)
                .rsvpStatus(RsvpStatus.NO_REPLY)
                .partySize(type.getAdmissionLimit())
                .admissionType(type)
                .admissionLimit(type.getAdmissionLimit())
                .usedAdmissions(0)
                .revoked(false)
                .build();

        Invitation saved = invitationRepository.save(invitation);
        try {
            saved = eCardRenderingEngineService.renderAndStoreCard(saved);
        } catch (Exception e) {
            System.err.println("Warning: Graphic card rendering failed: " + e.getMessage());
        }
        return invitationRepository.save(saved);
    }

    private boolean isUnsent(Invitation invitation) {
        DeliveryStatus delivery = invitation.getDeliveryStatus();
        InvitationStatus status = invitation.getStatus();
        if (status == InvitationStatus.SENT
                || status == InvitationStatus.DELIVERED
                || status == InvitationStatus.OPENED
                || status == InvitationStatus.USED) {
            return false;
        }
        return delivery == null
                || delivery == DeliveryStatus.PENDING
                || delivery == DeliveryStatus.FAILED;
    }

    private CheckInResponseDto checkInResponse(
            Invitation invitation,
            CheckInRequestDto request,
            CheckIn audit,
            CheckInResult result,
            String message,
            LocalDateTime now,
            CheckInEntitlementState entitlementState) {
        boolean belongs = request.getEventId() == null || request.getEventId().equals(invitation.getEventId());
        return CheckInResponseDto.builder()
                .checkInId(audit != null ? audit.getId() : null)
                .invitationId(invitation.getId())
                .eventId(invitation.getEventId())
                .token(request.getToken())
                .result(result)
                .message(message)
                .guestName(guestNameOf(invitation))
                .eventName(eventNameOf(invitation.getEventId()))
                .admissionType(AdmissionType.fromNullable(invitation.getAdmissionType()))
                .admissionLimit(invitation.resolvedAdmissionLimit())
                .usedAdmissions(invitation.resolvedUsedAdmissions())
                .remainingAdmissions(invitation.remainingAdmissions())
                .revoked(invitation.isRevoked() || invitation.getStatus() == InvitationStatus.REVOKED)
                .belongsToScannedEvent(belongs)
                .entitlementState(entitlementState)
                .scannedAt(now)
                .scannerId(request.getScannerId())
                .build();
    }

    private String guestNameOf(Invitation invitation) {
        if (invitation.getGuestId() == null) {
            return null;
        }
        return guestRepository.findById(invitation.getGuestId())
                .map(Guest::getFullName)
                .orElse(null);
    }

    private String eventNameOf(UUID eventId) {
        if (eventId == null) {
            return null;
        }
        return eventRepository.findById(eventId)
                .map(Event::getEventName)
                .orElse(null);
    }

    private String generateUniqueToken() {
        String token;
        do {
            token = tokenGeneratorService.generateSecureToken();
        } while (invitationRepository.existsByUniqueToken(token));
        return token;
    }

    private InvitationResponseDto mapToResponseDto(Invitation invitation) {
        InvitationResponseDto dto = new InvitationResponseDto();
        dto.setId(invitation.getId());
        dto.setEventId(invitation.getEventId());
        dto.setTemplateId(invitation.getTemplateId());
        dto.setTemplateVersion(invitation.getTemplateVersion() == null ? 1 : invitation.getTemplateVersion());
        dto.setGuestId(invitation.getGuestId());
        if (invitation.getGuestId() != null) {
            guestRepository.findById(invitation.getGuestId())
                    .ifPresent(g -> dto.setGuestName(g.getFullName()));
        }
        dto.setRecipientPhone(invitation.getRecipientPhone());
        dto.setRecipientEmail(invitation.getRecipientEmail());
        dto.setUniqueToken(invitation.getUniqueToken());
        dto.setQrCodeUrl(invitation.getQrCodeUrl());
        dto.setQrCode(invitation.getQrCode());
        dto.setCardReference(invitation.getCardReference());
        if (invitation.getId() != null) {
            dto.setCardUrl("/api/v1/invitations/" + invitation.getId() + "/card");
        }
        dto.setUsed(invitation.isUsed());
        dto.setScanned(invitation.isScanned());
        dto.setStatus(invitation.getStatus());
        dto.setDeliveryStatus(invitation.getDeliveryStatus());
        dto.setGeneratedAt(invitation.getGeneratedAt());
        dto.setExpiryDate(invitation.getExpiryDate());
        dto.setExpiresAt(invitation.getExpiresAt());
        dto.setScannedAt(invitation.getScannedAt());
        dto.setUsedAt(invitation.getUsedAt());
        dto.setAdmissionType(AdmissionType.fromNullable(invitation.getAdmissionType()));
        dto.setAdmissionLimit(invitation.resolvedAdmissionLimit());
        dto.setUsedAdmissions(invitation.resolvedUsedAdmissions());
        dto.setRemainingAdmissions(invitation.remainingAdmissions());
        dto.setRevoked(invitation.isRevoked() || invitation.getStatus() == InvitationStatus.REVOKED);
        return dto;
    }

    private InvitationDetailedResponseDto mapToDetailedResponseDto(Invitation invitation) {
        InvitationDetailedResponseDto dto = new InvitationDetailedResponseDto();
        dto.setId(invitation.getId());
        dto.setEventId(invitation.getEventId());
        dto.setTemplateId(invitation.getTemplateId());
        dto.setTemplateVersion(invitation.getTemplateVersion() == null ? 1 : invitation.getTemplateVersion());
        dto.setGuestId(invitation.getGuestId());
        if (invitation.getGuestId() != null) {
            guestRepository.findById(invitation.getGuestId())
                    .ifPresent(g -> dto.setGuestName(g.getFullName()));
        }
        dto.setRecipientPhone(invitation.getRecipientPhone());
        dto.setRecipientEmail(invitation.getRecipientEmail());
        dto.setUniqueToken(invitation.getUniqueToken());
        dto.setQrCodeUrl(invitation.getQrCodeUrl());
        dto.setQrCode(invitation.getQrCode());
        dto.setCardReference(invitation.getCardReference());
        if (invitation.getId() != null) {
            dto.setCardUrl("/api/v1/invitations/" + invitation.getId() + "/card");
        }
        dto.setUsed(invitation.isUsed());
        dto.setScanned(invitation.isScanned());
        dto.setStatus(invitation.getStatus());
        dto.setDeliveryStatus(invitation.getDeliveryStatus());
        dto.setSentAt(invitation.getSentAt());
        dto.setDeliveredAt(invitation.getDeliveredAt());
        dto.setOpenedAt(invitation.getOpenedAt());
        dto.setRsvpStatus(invitation.getRsvpStatus() == null ? RsvpStatus.NO_REPLY : invitation.getRsvpStatus());
        dto.setRsvpAt(invitation.getRsvpAt());
        dto.setPartySize(invitation.getPartySize() == null ? 1 : invitation.getPartySize());
        dto.setDietaryNotes(invitation.getDietaryNotes());
        dto.setMealChoice(invitation.getMealChoice());
        dto.setGeneratedAt(invitation.getGeneratedAt());
        dto.setExpiryDate(invitation.getExpiryDate());
        dto.setExpiresAt(invitation.getExpiresAt());
        dto.setScannedAt(invitation.getScannedAt());
        dto.setUsedAt(invitation.getUsedAt());
        dto.setBulkUploadSessionId(invitation.getBulkUploadSessionId());
        dto.setAdmissionType(AdmissionType.fromNullable(invitation.getAdmissionType()));
        dto.setAdmissionLimit(invitation.resolvedAdmissionLimit());
        dto.setUsedAdmissions(invitation.resolvedUsedAdmissions());
        dto.setRemainingAdmissions(invitation.remainingAdmissions());
        dto.setRevoked(invitation.isRevoked() || invitation.getStatus() == InvitationStatus.REVOKED);
        dto.setRevokedAt(invitation.getRevokedAt());
        return dto;
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
