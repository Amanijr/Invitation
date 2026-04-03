package com.InvitationSystem.InvitationSystem.Dto.bulkUploadDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@io.swagger.v3.oas.annotations.media.Schema(description = "Response payload for bulk upload processing results")
public class BulkUploadResponseDto {

    private UUID uploadSessionId;
    private UUID eventId;
    private String status;
    private int totalGuests;
    private int successCount;
    private int failureCount;
    private String errorMessage;
    private LocalDateTime uploadedAt;
    private LocalDateTime processedAt;
}
