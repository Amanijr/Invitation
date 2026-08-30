package com.InvitationSystem.InvitationSystem.Dto.deliveryDto;

import com.InvitationSystem.InvitationSystem.entity.DeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@io.swagger.v3.oas.annotations.media.Schema(description = "Aggregate response for multi-channel invitation delivery")
public class MultiChannelDeliveryResponseDto {

    private UUID invitationId;
    private UUID guestId;
    private String guestName;
    private DeliveryStatus overallStatus;
    private List<DeliveryResultDto> results;
}
