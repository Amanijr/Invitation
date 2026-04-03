package com.InvitationSystem.InvitationSystem.Dto.bulkUploadDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@io.swagger.v3.oas.annotations.media.Schema(description = "Response payload for bulk upload session metadata and status")
public class BulkUploadSessionDto {

    private UUID id;
    private UUID eventId;
    private UUID uploadedBy;
    private String fileName;
    private String fileType;
    private int totalGuests;
    private int successCount;
    private int failureCount;
    private String status;
    private String errorMessage;
    private LocalDateTime uploadedAt;
    private LocalDateTime processedAt;
}
