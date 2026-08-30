package com.InvitationSystem.InvitationSystem.service;

import com.InvitationSystem.InvitationSystem.Dto.checkInDto.CheckInRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.checkInDto.CheckInResponseDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationResponseDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationDetailedResponseDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationScanResponseDto;
import com.InvitationSystem.InvitationSystem.entity.InvitationStatus;

import java.util.List;
import java.util.UUID;

public interface InvitationService {

    InvitationResponseDto createInvitation(InvitationRequestDto request);

    InvitationDetailedResponseDto getInvitationById(UUID invitationId);

    InvitationDetailedResponseDto getInvitationByToken(String token);

    boolean validateInvitation(String token, String recipientPhone, String recipientEmail);

    List<InvitationDetailedResponseDto> getInvitationsByEvent(UUID eventId);

    List<InvitationDetailedResponseDto> getInvitationsByGuest(UUID guestId);

    List<InvitationResponseDto> getAllInvitations();

    List<InvitationResponseDto> getInvitationsForActor(UUID actorId, com.InvitationSystem.InvitationSystem.entity.UserRole role);

    List<InvitationDetailedResponseDto> getInvitationsByStatus(InvitationStatus status);

    InvitationDetailedResponseDto markAsSent(UUID invitationId);

    InvitationDetailedResponseDto markAsDelivered(UUID invitationId);

    InvitationDetailedResponseDto markAsOpened(UUID invitationId);

    InvitationDetailedResponseDto markAsUsed(UUID invitationId);

    InvitationDetailedResponseDto generateQrCode(UUID invitationId, String qrCodeUrl);

    InvitationScanResponseDto scanInvitationByToken(String token);

    CheckInResponseDto verifyInvitation(CheckInRequestDto request);

    java.util.List<com.InvitationSystem.InvitationSystem.Dto.checkInDto.CheckInHistoryDto> getCheckInHistory(UUID eventId);

    java.util.List<com.InvitationSystem.InvitationSystem.Dto.checkInDto.CheckInHistoryDto> getCheckInHistory(
            UUID eventId, UUID actorId, com.InvitationSystem.InvitationSystem.entity.UserRole actorRole);

    void deleteInvitation(UUID invitationId);

    List<InvitationDetailedResponseDto> getInvitationsByBulkUpload(UUID bulkUploadSessionId);

    com.InvitationSystem.InvitationSystem.Dto.invitationsDto.BulkGenerationResultDto generateBulkInvitations(
            com.InvitationSystem.InvitationSystem.Dto.invitationsDto.BulkGenerationRequestDto request);

    InvitationResponseDto issueInheritedInvitation(UUID eventId, UUID guestId, com.InvitationSystem.InvitationSystem.entity.AdmissionType admissionType);

    int regenerateInvitationsForTemplateChange(UUID eventId, UUID templateId, int templateVersion, com.InvitationSystem.InvitationSystem.entity.TemplateChangeScope scope);

    InvitationDetailedResponseDto revokeInvitation(UUID invitationId, UUID actorId, com.InvitationSystem.InvitationSystem.entity.UserRole actorRole);
}
