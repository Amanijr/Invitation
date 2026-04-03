package com.InvitationSystem.InvitationSystem.Dto.deliveryLogsDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@io.swagger.v3.oas.annotations.media.Schema(description = "Response payload for delivery log records")
public class DeliveryLogResponseDto {

    private UUID id;
    private UUID invitationId;
    private String channel;
    private String status;
    private String providerResponse;
    private int retryCount;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
}
