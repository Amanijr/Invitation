package com.InvitationSystem.InvitationSystem.service.impl;

import com.InvitationSystem.InvitationSystem.Dto.deliveryDto.*;
import com.InvitationSystem.InvitationSystem.Dto.deliveryLogsDto.DeliveryLogResponseDto;
import com.InvitationSystem.InvitationSystem.entity.*;
import com.InvitationSystem.InvitationSystem.provider.ChannelProvider;
import com.InvitationSystem.InvitationSystem.provider.DeliveryRequest;
import com.InvitationSystem.InvitationSystem.provider.DeliveryResult;
import com.InvitationSystem.InvitationSystem.repository.*;
import com.InvitationSystem.InvitationSystem.service.ECardRenderingEngineService;
import com.InvitationSystem.InvitationSystem.service.MultiChannelDeliveryService;
import com.InvitationSystem.InvitationSystem.service.storage.FileStorageService;
import com.InvitationSystem.InvitationSystem.util.GuestCardLinks;
import com.InvitationSystem.InvitationSystem.util.PDFService;
import com.InvitationSystem.InvitationSystem.util.PhoneNormalizationUtil;
import com.InvitationSystem.InvitationSystem.util.TemplateProcessorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MultiChannelDeliveryServiceImpl implements MultiChannelDeliveryService {

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private DeliveryLogRepository deliveryLogRepository;

    @Autowired
    private ECardRenderingEngineService eCardRenderingEngineService;

    @Autowired
    private TemplateProcessorService templateProcessorService;

    @Autowired
    private PDFService pdfService;

    @Autowired
    private FileStorageService fileStorageService;

    @Value("${app.public-url:http://localhost:5173}")
    private String publicUrl;

    private final Map<DeliveryChannel, ChannelProvider> providerMap = new EnumMap<>(DeliveryChannel.class);

    @Autowired
    public MultiChannelDeliveryServiceImpl(List<ChannelProvider> providers) {
        if (providers != null) {
            for (ChannelProvider provider : providers) {
                providerMap.put(provider.getChannel(), provider);
            }
        }
    }

    @Override
    @Transactional
    public MultiChannelDeliveryResponseDto sendInvitation(DeliveryRequestDto request) {
        if (request == null || request.getInvitationId() == null) {
            throw new IllegalArgumentException("Invitation ID is required for delivery");
        }

        if (request.getChannels() == null || request.getChannels().isEmpty()) {
            throw new IllegalArgumentException("At least one delivery channel must be selected");
        }

        Invitation invitation = invitationRepository.findById(request.getInvitationId())
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found with ID: " + request.getInvitationId()));

        Guest guest = null;
        if (invitation.getGuestId() != null) {
            guest = guestRepository.findById(invitation.getGuestId())
                    .orElseThrow(() -> new IllegalArgumentException("Guest not found with ID: " + invitation.getGuestId()));

            if (invitation.getEventId() != null && guest.getEventId() != null
                    && !invitation.getEventId().equals(guest.getEventId())) {
                throw new IllegalArgumentException("Guest does not belong to the specified event for this invitation");
            }
        }

        Event event = invitation.getEventId() != null
                ? eventRepository.findById(invitation.getEventId()).orElse(null)
                : null;

        Template template = invitation.getTemplateId() != null
                ? templateRepository.findById(invitation.getTemplateId()).orElse(null)
                : null;

        String guestName = guest != null && guest.getFullName() != null ? guest.getFullName() : "Guest";
        String eventName = event != null && event.getEventName() != null ? event.getEventName() : "Event";
        String eventDateStr = (event != null && event.getEventDate() != null)
                ? event.getEventDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                : "TBD";
        String venue = event != null && event.getVenue() != null ? event.getVenue() : "Venue";

        // Generate or retrieve card content
        String renderedHtml = buildRenderedHtml(invitation, guestName, eventName, eventDateStr, template);
        byte[] cardImageBytes = loadPressedCardPng(invitation);
        byte[] pdfBytes = cardImageBytes == null ? generatePdfBytes(renderedHtml) : null;

        String recipientEmail = (request.getRecipientEmail() != null && !request.getRecipientEmail().isBlank())
                ? request.getRecipientEmail()
                : (invitation.getRecipientEmail() != null ? invitation.getRecipientEmail() : (guest != null ? guest.getEmail() : null));

        String rawPhone = (request.getRecipientPhone() != null && !request.getRecipientPhone().isBlank())
                ? request.getRecipientPhone()
                : (invitation.getRecipientPhone() != null ? invitation.getRecipientPhone() : (guest != null ? guest.getPhone() : null));

        String recipientPhone = PhoneNormalizationUtil.normalizePhoneNumber(rawPhone);
        if (recipientPhone == null) {
            recipientPhone = rawPhone;
        }

        List<DeliveryResultDto> results = new ArrayList<>();
        boolean anySuccess = false;
        boolean allSuccess = true;

        for (DeliveryChannel channel : request.getChannels()) {
            String key = (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank())
                    ? request.getIdempotencyKey() + ":" + channel.name()
                    : "DELIVERY:" + invitation.getId() + ":" + channel.name();

            // Idempotency Check
            Optional<DeliveryLog> existingLogOpt = deliveryLogRepository.findByIdempotencyKey(key);
            if (existingLogOpt.isPresent()) {
                DeliveryLog existingLog = existingLogOpt.get();
                if ("SENT".equalsIgnoreCase(existingLog.getStatus()) || "DELIVERED".equalsIgnoreCase(existingLog.getStatus())) {
                    results.add(mapToResultDto(existingLog));
                    anySuccess = true;
                    continue;
                }
            }

            // Create or update log to PROCESSING
            DeliveryLog log = existingLogOpt.orElseGet(() -> DeliveryLog.builder()
                    .invitationId(invitation.getId())
                    .guestId(invitation.getGuestId())
                    .channel(channel.name())
                    .idempotencyKey(key)
                    .retryCount(0)
                    .build());

            log.setStatus("PROCESSING");
            log.setRecipientContact(channel == DeliveryChannel.EMAIL ? recipientEmail : recipientPhone);
            log = deliveryLogRepository.save(log);

            ChannelProvider provider = providerMap.get(channel);
            if (provider == null) {
                log.setStatus("FAILED");
                log.setErrorMessage("Unsupported or invalid channel: " + channel);
                log = deliveryLogRepository.save(log);
                results.add(mapToResultDto(log));
                allSuccess = false;
                continue;
            }

            DeliveryRequest deliveryReq = DeliveryRequest.builder()
                    .invitationId(invitation.getId())
                    .guestId(invitation.getGuestId())
                    .guestName(guestName)
                    .eventName(eventName)
                    .eventDate(eventDateStr)
                    .venue(venue)
                    .invitationToken(invitation.getUniqueToken())
                    .invitationUrl(GuestCardLinks.cardViewUrl(publicUrl, invitation.getUniqueToken()))
                    .qrCodeUrl(invitation.getQrCodeUrl())
                    .qrCodeBase64(invitation.getQrCode())
                    .cardReference(invitation.getCardReference())
                    .renderedHtml(renderedHtml)
                    .cardImageBytes(cardImageBytes)
                    .cardImageFileName("invitation-card.png")
                    .pdfBytes(pdfBytes)
                    .pdfFileName("invitation-card-" + invitation.getId() + ".pdf")
                    .recipientEmail(recipientEmail)
                    .recipientPhone(recipientPhone)
                    .channel(channel)
                    .idempotencyKey(key)
                    .build();

            DeliveryResult result = provider.send(deliveryReq);

            log.setStatus(result.getStatus().name());
            log.setProviderReference(result.getProviderReference());
            log.setProviderResponse(result.getProviderResponse());
            log.setErrorMessage(result.getErrorMessage());
            log.setSentAt(LocalDateTime.now());
            if (result.getStatus() == DeliveryStatus.DELIVERED) {
                log.setDeliveredAt(LocalDateTime.now());
            }

            log = deliveryLogRepository.save(log);
            results.add(mapToResultDto(log));

            if (result.isSuccess()) {
                anySuccess = true;
            } else {
                allSuccess = false;
            }
        }

        // Update Invitation overall status
        if (anySuccess) {
            invitation.setStatus(InvitationStatus.SENT);
            if (request.getChannels().contains(DeliveryChannel.EMAIL)) {
                invitation.setDeliveryStatus(DeliveryStatus.SENT_EMAIL);
            } else if (request.getChannels().contains(DeliveryChannel.WHATSAPP)) {
                invitation.setDeliveryStatus(DeliveryStatus.SENT_WHATSAPP);
            } else {
                invitation.setDeliveryStatus(DeliveryStatus.SENT);
            }
            invitation.setSentAt(LocalDateTime.now());
        } else {
            invitation.setStatus(InvitationStatus.FAILED);
            invitation.setDeliveryStatus(DeliveryStatus.FAILED);
        }
        invitationRepository.save(invitation);

        DeliveryStatus overallStatus = anySuccess
                ? (allSuccess ? DeliveryStatus.SENT : DeliveryStatus.SENT)
                : DeliveryStatus.FAILED;

        return MultiChannelDeliveryResponseDto.builder()
                .invitationId(invitation.getId())
                .guestId(invitation.getGuestId())
                .guestName(guestName)
                .overallStatus(overallStatus)
                .results(results)
                .build();
    }

    @Override
    @Transactional
    public BatchDeliveryResponseDto sendBatchInvitations(BatchDeliveryRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Batch delivery request cannot be null");
        }

        List<Invitation> targetInvitations;
        if (request.getInvitationIds() != null && !request.getInvitationIds().isEmpty()) {
            targetInvitations = invitationRepository.findAllById(request.getInvitationIds());
        } else if (request.getEventId() != null) {
            targetInvitations = invitationRepository.findByEventId(request.getEventId());
        } else {
            throw new IllegalArgumentException("Either invitationIds or eventId must be provided");
        }

        int total = targetInvitations.size();
        int successCount = 0;
        int failedCount = 0;
        List<MultiChannelDeliveryResponseDto> invitationResults = new ArrayList<>();

        String prefix = request.getIdempotencyPrefix() != null ? request.getIdempotencyPrefix() : "BATCH-" + UUID.randomUUID().toString().substring(0, 8);

        for (Invitation invitation : targetInvitations) {
            try {
                DeliveryRequestDto singleReq = DeliveryRequestDto.builder()
                        .invitationId(invitation.getId())
                        .channels(request.getChannels())
                        .idempotencyKey(prefix + ":" + invitation.getId())
                        .build();

                MultiChannelDeliveryResponseDto resp = sendInvitation(singleReq);
                invitationResults.add(resp);

                if (resp.getOverallStatus() == DeliveryStatus.SENT || resp.getOverallStatus() == DeliveryStatus.DELIVERED) {
                    successCount++;
                } else {
                    failedCount++;
                }
            } catch (Exception e) {
                failedCount++;
                invitationResults.add(MultiChannelDeliveryResponseDto.builder()
                        .invitationId(invitation.getId())
                        .guestId(invitation.getGuestId())
                        .overallStatus(DeliveryStatus.FAILED)
                        .results(Collections.emptyList())
                        .build());
            }
        }

        return BatchDeliveryResponseDto.builder()
                .totalInvitations(total)
                .successCount(successCount)
                .failedCount(failedCount)
                .invitationResults(invitationResults)
                .processedAt(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public DeliveryLogResponseDto retryDelivery(UUID logId) {
        DeliveryLog log = deliveryLogRepository.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("Delivery log not found with ID: " + logId));

        Invitation invitation = invitationRepository.findById(log.getInvitationId())
                .orElseThrow(() -> new IllegalArgumentException("Associated invitation not found: " + log.getInvitationId()));

        DeliveryChannel channel;
        try {
            channel = DeliveryChannel.valueOf(log.getChannel().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid channel in delivery log: " + log.getChannel());
        }

        log.setRetryCount(log.getRetryCount() + 1);
        log.setStatus("PROCESSING");
        DeliveryLog savedLog = deliveryLogRepository.save(log);

        DeliveryRequestDto request = DeliveryRequestDto.builder()
                .invitationId(invitation.getId())
                .channels(List.of(channel))
                .idempotencyKey(savedLog.getIdempotencyKey() + ":RETRY-" + savedLog.getRetryCount())
                .build();

        MultiChannelDeliveryResponseDto resp = sendInvitation(request);

        Optional<DeliveryLog> updatedLogOpt = deliveryLogRepository.findById(logId);
        return mapToResponseDto(updatedLogOpt.orElse(savedLog));
    }

    @Override
    public List<DeliveryLogResponseDto> getLogsByInvitation(UUID invitationId) {
        return mapLogs(deliveryLogRepository.findByInvitationId(invitationId));
    }

    @Override
    public List<DeliveryLogResponseDto> getAllLogs() {
        return mapLogs(deliveryLogRepository.findAll());
    }

    @Override
    public List<DeliveryLogResponseDto> getLogsForActor(UUID actorId, UserRole role) {
        if (role == UserRole.ADMIN) {
            return getAllLogs();
        }
        List<UUID> eventIds = eventRepository.findByCreatedBy(actorId).stream()
                .map(Event::getId)
                .toList();
        if (eventIds.isEmpty()) {
            return List.of();
        }
        List<UUID> invitationIds = invitationRepository.findByEventIdIn(eventIds).stream()
                .map(Invitation::getId)
                .toList();
        if (invitationIds.isEmpty()) {
            return List.of();
        }
        return mapLogs(deliveryLogRepository.findByInvitationIdIn(invitationIds));
    }

    private String buildRenderedHtml(Invitation invitation, String guestName, String eventName, String eventDateStr, Template template) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("guestName", guestName);
        placeholders.put("eventName", eventName);
        placeholders.put("eventDate", eventDateStr);
        placeholders.put("invitationUrl", GuestCardLinks.cardViewUrl(publicUrl, invitation.getUniqueToken()));
        if (invitation.getQrCode() != null) {
            placeholders.put("qrCode", "<img src=\"data:image/png;base64," + invitation.getQrCode() + "\" width=\"200\" alt=\"QR Code\" />");
        }

        String content = (template != null && template.getContent() != null && !template.getContent().isBlank())
                ? template.getContent()
                : null;

        // Catalog rows store JSON metadata in content — that must never be emailed as the card.
        if (content != null && content.stripLeading().startsWith("<")) {
            return templateProcessorService.renderTemplate(content, placeholders);
        }

        return null;
    }

    private byte[] loadPressedCardPng(Invitation invitation) {
        try {
            if (invitation.getCardReference() != null && fileStorageService != null
                    && fileStorageService.exists(invitation.getCardReference())) {
                return fileStorageService.loadFile(invitation.getCardReference());
            }
            if (eCardRenderingEngineService != null) {
                return eCardRenderingEngineService.renderCardImageBytes(invitation.getId());
            }
        } catch (Exception e) {
            System.err.println("Warning: could not load pressed card for delivery: " + e.getMessage());
        }
        return null;
    }

    private byte[] generatePdfBytes(String html) {
        if (html == null || html.isBlank() || !html.stripLeading().startsWith("<")) {
            return null;
        }
        try {
            String pdfBase64 = pdfService.generateInvitationCardPdf(html);
            if (pdfBase64 != null && !pdfBase64.isBlank()) {
                return Base64.getDecoder().decode(pdfBase64);
            }
        } catch (Exception e) {
            System.err.println("Warning: PDF generation for delivery attachment failed: " + e.getMessage());
        }
        return null;
    }

    private DeliveryResultDto mapToResultDto(DeliveryLog log) {
        DeliveryChannel channelEnum = null;
        try {
            channelEnum = DeliveryChannel.valueOf(log.getChannel().toUpperCase());
        } catch (Exception ignored) {}

        DeliveryStatus statusEnum = null;
        try {
            statusEnum = DeliveryStatus.valueOf(log.getStatus().toUpperCase());
        } catch (Exception ignored) {}

        return DeliveryResultDto.builder()
                .logId(log.getId())
                .invitationId(log.getInvitationId())
                .guestId(log.getGuestId())
                .channel(channelEnum)
                .status(statusEnum)
                .recipientContact(log.getRecipientContact())
                .attemptCount(log.getRetryCount())
                .providerReference(log.getProviderReference())
                .providerResponse(log.getProviderResponse())
                .errorMessage(log.getErrorMessage())
                .idempotencyKey(log.getIdempotencyKey())
                .sentAt(log.getSentAt())
                .deliveredAt(log.getDeliveredAt())
                .build();
    }

    private List<DeliveryLogResponseDto> mapLogs(List<DeliveryLog> logs) {
        Set<UUID> guestIds = logs.stream()
                .map(DeliveryLog::getGuestId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, Guest> guests = guestIds.isEmpty()
                ? Map.of()
                : guestRepository.findAllById(guestIds).stream()
                .collect(Collectors.toMap(Guest::getId, guest -> guest));
        return logs.stream()
                .map(log -> mapToResponseDto(log, log.getGuestId() == null ? null : guests.get(log.getGuestId())))
                .collect(Collectors.toList());
    }

    private DeliveryLogResponseDto mapToResponseDto(DeliveryLog log) {
        Guest guest = null;
        if (log.getGuestId() != null) {
            guest = guestRepository.findById(log.getGuestId()).orElse(null);
        }
        return mapToResponseDto(log, guest);
    }

    private DeliveryLogResponseDto mapToResponseDto(DeliveryLog log, Guest guest) {
        return DeliveryLogResponseDto.builder()
                .id(log.getId())
                .invitationId(log.getInvitationId())
                .guestId(log.getGuestId())
                .channel(log.getChannel())
                .status(log.getStatus())
                .recipientContact(log.getRecipientContact())
                .guestName(guest != null ? guest.getFullName() : null)
                .providerReference(log.getProviderReference())
                .providerResponse(log.getProviderResponse())
                .errorMessage(log.getErrorMessage())
                .idempotencyKey(log.getIdempotencyKey())
                .retryCount(log.getRetryCount())
                .sentAt(log.getSentAt())
                .deliveredAt(log.getDeliveredAt())
                .createdAt(log.getCreatedAt())
                .updatedAt(log.getUpdatedAt())
                .build();
    }
}
