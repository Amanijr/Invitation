package com.InvitationSystem.InvitationSystem.service.impl;

import com.InvitationSystem.InvitationSystem.Dto.bulkUploadDto.BulkUploadRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.bulkUploadDto.BulkUploadResponseDto;
import com.InvitationSystem.InvitationSystem.Dto.bulkUploadDto.BulkUploadSessionDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationRequestDto;
import com.InvitationSystem.InvitationSystem.entity.BulkUploadSession;
import com.InvitationSystem.InvitationSystem.entity.UploadStatus;
import com.InvitationSystem.InvitationSystem.repository.BulkUploadSessionRepository;
import com.InvitationSystem.InvitationSystem.repository.InvitationRepository;
import com.InvitationSystem.InvitationSystem.service.BulkUploadService;
import com.InvitationSystem.InvitationSystem.service.InvitationService;
import com.InvitationSystem.InvitationSystem.util.ExcelParserService;
import com.InvitationSystem.InvitationSystem.util.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class BulkUploadServiceImpl implements BulkUploadService {

    @Autowired
    private BulkUploadSessionRepository bulkUploadSessionRepository;

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private InvitationService invitationService;

    @Autowired
    private ExcelParserService excelParserService;

    @Autowired
    private EmailService emailService;

    @Override
    public BulkUploadResponseDto processBulkUpload(BulkUploadRequestDto request, UUID uploadedBy) {
        BulkUploadSession session = BulkUploadSession.builder()
                .eventId(request.getEventId())
                .uploadedBy(uploadedBy)
                .fileName(request.getFileName())
                .fileType(request.getFileType())
                .status(UploadStatus.PROCESSING)
                .totalGuests(0)
                .successCount(0)
                .failureCount(0)
                .build();

        BulkUploadSession savedSession = bulkUploadSessionRepository.save(session);

        try {
            List<Map<String, String>> guestData = parseFile(request.getFileContent(), request.getFileType());
            savedSession.setTotalGuests(guestData.size());

            int successCount = 0;
            int failureCount = 0;
            StringBuilder errorMessage = new StringBuilder();

            for (Map<String, String> guest : guestData) {
                try {
                    InvitationRequestDto invitationRequest = new InvitationRequestDto();
                    invitationRequest.setEventId(request.getEventId());
                    invitationRequest.setTemplateId(request.getTemplateId() != null ? request.getTemplateId() : null);
                    invitationRequest.setRecipientPhone(guest.get("phone"));
                    invitationRequest.setRecipientEmail(guest.get("email"));
                    invitationRequest.setExpiryDate(LocalDateTime.now().plusDays(30));

                    UUID guestId = UUID.nameUUIDFromBytes((guest.get("email") + guest.get("phone")).getBytes());
                    invitationRequest.setGuestId(guestId);

                    var invitation = invitationService.createInvitation(invitationRequest);

                    invitationRepository.findByUniqueToken(invitation.getUniqueToken())
                            .ifPresent(inv -> {
                                inv.setBulkUploadSessionId(savedSession.getId());
                                invitationRepository.save(inv);
                            });

                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    errorMessage.append("Guest: ").append(guest.get("email")).append(" - Error: ").append(e.getMessage()).append("; ");
                }
            }

            savedSession.setSuccessCount(successCount);
            savedSession.setFailureCount(failureCount);
            savedSession.setStatus(UploadStatus.COMPLETED);
            if (failureCount > 0) {
                savedSession.setErrorMessage(errorMessage.toString());
            }

            BulkUploadSession completedSession = bulkUploadSessionRepository.save(savedSession);
            
            // Send confirmation email
            try {
                emailService.sendBulkUploadConfirmation(
                    "organizer@email.com",
                    request.getEventId().toString(),
                    successCount,
                    failureCount
                );
            } catch (Exception e) {
                // Log email error but don't fail the upload
                System.err.println("Failed to send bulk upload confirmation email: " + e.getMessage());
            }
            
            return mapToResponseDto(completedSession);

        } catch (Exception e) {
            savedSession.setStatus(UploadStatus.FAILED);
            savedSession.setErrorMessage(e.getMessage());
            bulkUploadSessionRepository.save(savedSession);
            throw new RuntimeException("Failed to process bulk upload: " + e.getMessage());
        }
    }

    @Override
    public BulkUploadSessionDto getUploadSession(UUID sessionId) {
        BulkUploadSession session = bulkUploadSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Upload session not found with ID: " + sessionId));
        return mapToSessionDto(session);
    }

    @Override
    public List<BulkUploadSessionDto> getUploadSessionsByEvent(UUID eventId) {
        return bulkUploadSessionRepository.findByEventIdOrderByUploadedAtDesc(eventId).stream()
                .map(this::mapToSessionDto)
                .toList();
    }

    @Override
    public List<BulkUploadSessionDto> getUploadSessionsByUser(UUID uploadedBy) {
        return bulkUploadSessionRepository.findByUploadedByOrderByUploadedAtDesc(uploadedBy).stream()
                .map(this::mapToSessionDto)
                .toList();
    }

    @Override
    public List<BulkUploadSessionDto> getAllUploadSessions() {
        return bulkUploadSessionRepository.findAll().stream()
                .map(this::mapToSessionDto)
                .toList();
    }

    private List<Map<String, String>> parseFile(byte[] fileContent, String fileType) {
        List<Map<String, String>> guestData = new ArrayList<>();

        if ("EXCEL".equalsIgnoreCase(fileType)) {
            guestData = parseExcel(fileContent);
        } else if ("PDF".equalsIgnoreCase(fileType)) {
            guestData = parsePdf(fileContent);
        } else {
            throw new IllegalArgumentException("Unsupported file type: " + fileType);
        }

        if (guestData.isEmpty()) {
            throw new IllegalArgumentException("No guest data found in the uploaded file");
        }

        return guestData;
    }

    private List<Map<String, String>> parseExcel(byte[] fileContent) {
        return excelParserService.parseExcelBytes(fileContent);
    }

    private List<Map<String, String>> parsePdf(byte[] fileContent) {
        // PDF parsing is more complex and requires specific structure knowledge
        // For now, throw an exception - can be implemented based on your PDF structure
        throw new IllegalArgumentException("PDF parsing not yet implemented. Please use Excel format for bulk uploads.");
    }

    private BulkUploadResponseDto mapToResponseDto(BulkUploadSession session) {
        BulkUploadResponseDto dto = new BulkUploadResponseDto();
        dto.setUploadSessionId(session.getId());
        dto.setEventId(session.getEventId());
        dto.setStatus(session.getStatus().toString());
        dto.setTotalGuests(session.getTotalGuests());
        dto.setSuccessCount(session.getSuccessCount());
        dto.setFailureCount(session.getFailureCount());
        dto.setErrorMessage(session.getErrorMessage());
        dto.setUploadedAt(session.getUploadedAt());
        dto.setProcessedAt(session.getProcessedAt());
        return dto;
    }

    private BulkUploadSessionDto mapToSessionDto(BulkUploadSession session) {
        BulkUploadSessionDto dto = new BulkUploadSessionDto();
        dto.setId(session.getId());
        dto.setEventId(session.getEventId());
        dto.setUploadedBy(session.getUploadedBy());
        dto.setFileName(session.getFileName());
        dto.setFileType(session.getFileType());
        dto.setTotalGuests(session.getTotalGuests());
        dto.setSuccessCount(session.getSuccessCount());
        dto.setFailureCount(session.getFailureCount());
        dto.setStatus(session.getStatus().toString());
        dto.setErrorMessage(session.getErrorMessage());
        dto.setUploadedAt(session.getUploadedAt());
        dto.setProcessedAt(session.getProcessedAt());
        return dto;
    }
}
