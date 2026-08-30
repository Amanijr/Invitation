package com.InvitationSystem.InvitationSystem.Dto.deliveryDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@io.swagger.v3.oas.annotations.media.Schema(description = "Response payload for batch multi-channel invitation delivery")
public class BatchDeliveryResponseDto {

    private int totalInvitations;
    private int successCount;
    private int failedCount;
    private List<MultiChannelDeliveryResponseDto> invitationResults;
    private LocalDateTime processedAt;
}
