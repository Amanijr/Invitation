package com.InvitationSystem.InvitationSystem.Dto.deliveryDto;

import com.InvitationSystem.InvitationSystem.entity.DeliveryChannel;
import jakarta.validation.constraints.NotEmpty;
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
@io.swagger.v3.oas.annotations.media.Schema(description = "Request payload for sending batch invitations across channels")
public class BatchDeliveryRequestDto {

    private UUID eventId;

    private List<UUID> invitationIds;

    @NotEmpty(message = "channels list cannot be empty")
    private List<DeliveryChannel> channels;

    private String idempotencyPrefix;
}
