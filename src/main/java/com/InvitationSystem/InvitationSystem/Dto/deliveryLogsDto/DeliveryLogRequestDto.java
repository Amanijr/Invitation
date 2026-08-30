package com.InvitationSystem.InvitationSystem.Dto.deliveryLogsDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@io.swagger.v3.oas.annotations.media.Schema(description = "Request payload for creating a delivery log record")
public class DeliveryLogRequestDto {

    private UUID invitationId;
    private UUID guestId;
    private String channel;
    private String status;
    private String recipientContact;
    private String providerReference;
    private String providerResponse;
    private String errorMessage;
    private String idempotencyKey;

    public DeliveryLogRequestDto(UUID invitationId, String channel, String status) {
        this.invitationId = invitationId;
        this.channel = channel;
        this.status = status;
    }
}
