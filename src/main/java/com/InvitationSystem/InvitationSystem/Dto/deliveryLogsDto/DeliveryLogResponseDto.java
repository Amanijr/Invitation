package com.InvitationSystem.InvitationSystem.Dto.deliveryLogsDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@io.swagger.v3.oas.annotations.media.Schema(description = "Response payload for delivery log records")
public class DeliveryLogResponseDto {

    private UUID id;
    private UUID invitationId;
    private UUID guestId;
    private String channel;
    private String status;
    private String recipientContact;
    private String guestName;
    private String providerReference;
    private String providerResponse;
    private String errorMessage;
    private String idempotencyKey;
    private int retryCount;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
