package com.InvitationSystem.InvitationSystem.service;

import com.InvitationSystem.InvitationSystem.Dto.bulkUploadDto.BulkUploadRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.bulkUploadDto.BulkUploadResponseDto;
import com.InvitationSystem.InvitationSystem.Dto.bulkUploadDto.BulkUploadSessionDto;

import java.util.List;
import java.util.UUID;

public interface BulkUploadService {

    BulkUploadResponseDto processBulkUpload(BulkUploadRequestDto request, UUID uploadedBy);

    BulkUploadSessionDto getUploadSession(UUID sessionId);

    List<BulkUploadSessionDto> getUploadSessionsByEvent(UUID eventId);

    List<BulkUploadSessionDto> getUploadSessionsByUser(UUID uploadedBy);

    List<BulkUploadSessionDto> getAllUploadSessions();
}
