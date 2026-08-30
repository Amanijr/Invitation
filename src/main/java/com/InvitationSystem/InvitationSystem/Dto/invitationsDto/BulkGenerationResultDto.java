package com.InvitationSystem.InvitationSystem.Dto.invitationsDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkGenerationResultDto {
    private UUID eventId;
    private UUID templateId;
    private int totalGuests;
    private int successCount;
    private int failedCount;
    private int skippedCount;
    private List<UUID> successfulInvitationIds;
    private List<BulkGenerationErrorDto> errors;
    private LocalDateTime processedAt;
}
