package com.InvitationSystem.InvitationSystem.service;

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

    List<InvitationDetailedResponseDto> getInvitationsByStatus(InvitationStatus status);

    InvitationDetailedResponseDto markAsSent(UUID invitationId);

    InvitationDetailedResponseDto markAsDelivered(UUID invitationId);

    InvitationDetailedResponseDto markAsOpened(UUID invitationId);

    InvitationDetailedResponseDto markAsUsed(UUID invitationId);

    InvitationDetailedResponseDto generateQrCode(UUID invitationId, String qrCodeUrl);

    InvitationScanResponseDto scanInvitationByToken(String token);

    void deleteInvitation(UUID invitationId);

    List<InvitationDetailedResponseDto> getInvitationsByBulkUpload(UUID bulkUploadSessionId);
}
