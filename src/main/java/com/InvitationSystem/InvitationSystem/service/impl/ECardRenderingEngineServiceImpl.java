package com.InvitationSystem.InvitationSystem.service.impl;

import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.BatchRenderResultDto;
import com.InvitationSystem.InvitationSystem.Dto.templatesDto.TemplateFieldConfigDto;
import com.InvitationSystem.InvitationSystem.entity.*;
import com.InvitationSystem.InvitationSystem.repository.EventRepository;
import com.InvitationSystem.InvitationSystem.repository.GuestRepository;
import com.InvitationSystem.InvitationSystem.repository.InvitationRepository;
import com.InvitationSystem.InvitationSystem.repository.TemplateRepository;
import com.InvitationSystem.InvitationSystem.service.ECardRenderingEngineService;
import com.InvitationSystem.InvitationSystem.service.TemplateFieldConfigService;
import com.InvitationSystem.InvitationSystem.service.TemplateService;
import com.InvitationSystem.InvitationSystem.service.storage.FileStorageService;
import com.InvitationSystem.InvitationSystem.util.AdmissionDisplayName;
import com.InvitationSystem.InvitationSystem.util.ImageCardGeneratorService;
import com.InvitationSystem.InvitationSystem.util.QRCodeService;
import com.InvitationSystem.InvitationSystem.util.TokenGeneratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class ECardRenderingEngineServiceImpl implements ECardRenderingEngineService {

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private TemplateFieldConfigService fieldConfigService;

    @Autowired
    private ImageCardGeneratorService imageCardGeneratorService;

    @Autowired
    private QRCodeService qrCodeService;

    @Autowired
    private TokenGeneratorService tokenGeneratorService;

    @Autowired
    private FileStorageService fileStorageService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    @Override
    @Transactional
    public Invitation renderAndStoreCard(UUID invitationId) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found with ID: " + invitationId));
        return renderAndStoreCard(invitation);
    }

    @Override
    @Transactional
    public Invitation renderAndStoreCard(Invitation invitation) {
        if (invitation == null) {
            throw new IllegalArgumentException("Invitation cannot be null");
        }

        // 1. Ensure unique token and scan URL exist
        if (invitation.getUniqueToken() == null || invitation.getUniqueToken().isBlank()) {
            invitation.setUniqueToken(generateUniqueToken());
        }
        String scanUrl = baseUrl + "/api/v1/invitations/scan/" + invitation.getUniqueToken();
        invitation.setQrCodeUrl(scanUrl);

        // 2. Generate Base64 QR Code
        String qrBase64 = qrCodeService.generateQRCodeImage(scanUrl);
        invitation.setQrCode(qrBase64);

        // 3. Load associated entities
        Guest guest = guestRepository.findById(invitation.getGuestId()).orElse(null);
        Event event = eventRepository.findById(invitation.getEventId()).orElse(null);
        Template template = templateRepository.findById(invitation.getTemplateId()).orElse(null);

        // 4. Assemble dynamic data map
        Map<String, String> dataMap = buildDataMap(guest, event, invitation, scanUrl);

        // 5. Load name + QR slots. Event copy stays in the artwork.
        List<TemplateFieldConfigDto> configs = pressFields(invitation.getTemplateId());

        // 6. Load background image bytes
        byte[] bgBytes = null;
        if (template != null && template.getStoragePath() != null) {
            try {
                bgBytes = templateService.loadTemplateFile(template.getId());
            } catch (Exception e) {
                log.warn("Failed to load background template image for template {}: {}", template.getId(), e.getMessage());
            }
        }

        // 7. Render high-res composite PNG image
        byte[] cardPngBytes = imageCardGeneratorService.renderCardImage(bgBytes, configs, dataMap);

        // 8. Save rendered E-Card file to storage with secure UUID filename
        String secureFileName = "ecard-" + UUID.randomUUID().toString() + ".png";
        ByteArrayMultipartFile fileToSave = new ByteArrayMultipartFile(
                "file",
                secureFileName,
                "image/png",
                cardPngBytes
        );

        var metadata = fileStorageService.storeFile(fileToSave, "CARDS");
        invitation.setCardReference(metadata.getStoragePath());

        // 9. Save and return updated invitation
        return invitationRepository.save(invitation);
    }

    private static class ByteArrayMultipartFile implements org.springframework.web.multipart.MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        public ByteArrayMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content != null ? content : new byte[0];
        }

        @Override public String getName() { return name; }
        @Override public String getOriginalFilename() { return originalFilename; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return content.length == 0; }
        @Override public long getSize() { return content.length; }
        @Override public byte[] getBytes() { return content; }
        @Override public java.io.InputStream getInputStream() { return new java.io.ByteArrayInputStream(content); }
        @Override public void transferTo(java.io.File dest) throws java.io.IOException, IllegalStateException {
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(dest)) {
                fos.write(content);
            }
        }
    }

    @Override
    public BatchRenderResultDto renderBatchForEvent(UUID eventId) {
        List<Invitation> invitations = invitationRepository.findByEventId(eventId);
        List<UUID> ids = invitations.stream().map(Invitation::getId).toList();
        return renderBatch(ids);
    }

    @Override
    public BatchRenderResultDto renderBatch(List<UUID> invitationIds) {
        if (invitationIds == null || invitationIds.isEmpty()) {
            return BatchRenderResultDto.builder()
                    .totalCount(0)
                    .successCount(0)
                    .failureCount(0)
                    .errorLogs(List.of())
                    .failedInvitationIds(List.of())
                    .build();
        }

        int total = invitationIds.size();
        int successCount = 0;
        int failureCount = 0;
        List<String> errorLogs = new ArrayList<>();
        List<UUID> failedIds = new ArrayList<>();

        for (UUID id : invitationIds) {
            try {
                renderAndStoreCard(id);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                failedIds.add(id);
                String errorMsg = "Invitation ID " + id + " failed: " + e.getMessage();
                errorLogs.add(errorMsg);
                log.error(errorMsg, e);

                // Update invitation status to FAILED if possible
                try {
                    invitationRepository.findById(id).ifPresent(inv -> {
                        inv.setStatus(InvitationStatus.FAILED);
                        inv.setDeliveryStatus(DeliveryStatus.FAILED);
                        invitationRepository.save(inv);
                    });
                } catch (Exception ex) {
                    // Ignore secondary save failure
                }
            }
        }

        return BatchRenderResultDto.builder()
                .totalCount(total)
                .successCount(successCount)
                .failureCount(failureCount)
                .errorLogs(errorLogs)
                .failedInvitationIds(failedIds)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] renderCardImageBytesByToken(String uniqueToken) {
        if (uniqueToken == null || uniqueToken.isBlank()) {
            throw new IllegalArgumentException("Invitation token is required");
        }
        Invitation invitation = invitationRepository.findByUniqueToken(uniqueToken.trim())
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found for token"));
        return renderCardImageBytes(invitation.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] renderCardImageBytes(UUID invitationId) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found with ID: " + invitationId));

        if (invitation.getCardReference() != null && fileStorageService.exists(invitation.getCardReference())) {
            return fileStorageService.loadFile(invitation.getCardReference());
        }

        // On-the-fly rendering if card file does not exist on disk
        Guest guest = guestRepository.findById(invitation.getGuestId()).orElse(null);
        Event event = eventRepository.findById(invitation.getEventId()).orElse(null);
        Template template = templateRepository.findById(invitation.getTemplateId()).orElse(null);

        String scanUrl = invitation.getQrCodeUrl();
        if (scanUrl == null || scanUrl.isBlank()) {
            scanUrl = baseUrl + "/api/v1/invitations/scan/" + invitation.getUniqueToken();
        }

        Map<String, String> dataMap = buildDataMap(guest, event, invitation, scanUrl);
        List<TemplateFieldConfigDto> configs = pressFields(invitation.getTemplateId());

        byte[] bgBytes = null;
        if (template != null && template.getStoragePath() != null) {
            try {
                bgBytes = templateService.loadTemplateFile(template.getId());
            } catch (Exception e) {
                // Handled internally by renderer
            }
        }

        return imageCardGeneratorService.renderCardImage(bgBytes, configs, dataMap);
    }

    private Map<String, String> buildDataMap(Guest guest, Event event, Invitation invitation, String scanUrl) {
        Map<String, String> dataMap = new HashMap<>();

        String guestName = "Valued Guest";
        if (guest != null && guest.getFullName() != null && !guest.getFullName().isBlank()) {
            guestName = guest.getFullName();
        } else if (invitation.getRecipientEmail() != null && !invitation.getRecipientEmail().isBlank()) {
            guestName = invitation.getRecipientEmail();
        }
        dataMap.put("GUEST_NAME", AdmissionDisplayName.forGuest(guestName, invitation.getAdmissionType()));

        String eventName = "Special Event";
        String eventDateStr = "Date TBD";
        String eventTimeStr = "Time TBD";
        String venueStr = "Venue TBD";

        if (event != null) {
            if (event.getEventName() != null) eventName = event.getEventName();
            if (event.getVenue() != null) venueStr = event.getVenue();
            if (event.getEventDate() != null) {
                eventDateStr = event.getEventDate().format(DATE_FORMATTER);
                eventTimeStr = event.getEventDate().format(TIME_FORMATTER);
            }
        }

        dataMap.put("EVENT_NAME", eventName);
        dataMap.put("EVENT_DATE", eventDateStr);
        dataMap.put("EVENT_TIME", eventTimeStr);
        dataMap.put("EVENT_VENUE", venueStr);
        dataMap.put("QR_CODE", scanUrl);

        return dataMap;
    }

    private List<TemplateFieldConfigDto> pressFields(UUID templateId) {
        List<TemplateFieldConfigDto> configs = fieldConfigService.getFieldConfigsByTemplateId(templateId);
        List<TemplateFieldConfigDto> press = configs == null ? List.of() : configs.stream()
                .filter(config -> config.getFieldType() == FieldType.GUEST_NAME
                        || config.getFieldType() == FieldType.QR_CODE)
                .toList();
        if (press.isEmpty()) {
            return buildDefaultFieldConfigs(templateId);
        }
        return press;
    }

    private List<TemplateFieldConfigDto> buildDefaultFieldConfigs(UUID templateId) {
        List<TemplateFieldConfigDto> defaults = new ArrayList<>();

        defaults.add(TemplateFieldConfigDto.builder()
                .templateId(templateId)
                .fieldType(FieldType.GUEST_NAME)
                .x(12.0).y(28.0).width(76.0).height(8.0)
                .fontSize(32).fontColor("#111318").alignment("CENTER").fontWeight("BOLD")
                .build());

        defaults.add(TemplateFieldConfigDto.builder()
                .templateId(templateId)
                .fieldType(FieldType.QR_CODE)
                .x(38.0).y(78.0).width(24.0).height(18.0)
                .qrSize(220)
                .build());

        return defaults;
    }

    private String generateUniqueToken() {
        String token;
        do {
            token = tokenGeneratorService.generateSecureToken();
        } while (invitationRepository.existsByUniqueToken(token));
        return token;
    }
}
