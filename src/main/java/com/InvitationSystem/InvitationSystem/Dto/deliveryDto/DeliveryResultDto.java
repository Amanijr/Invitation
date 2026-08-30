package com.InvitationSystem.InvitationSystem.Dto.deliveryDto;

import com.InvitationSystem.InvitationSystem.entity.DeliveryChannel;
import com.InvitationSystem.InvitationSystem.entity.DeliveryStatus;
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
@io.swagger.v3.oas.annotations.media.Schema(description = "Individual channel delivery result")
public class DeliveryResultDto {

    private UUID logId;
    private UUID invitationId;
    private UUID guestId;
    private DeliveryChannel channel;
    private DeliveryStatus status;
    private String recipientContact;
    private int attemptCount;
    private String providerReference;
    private String providerResponse;
    private String errorMessage;
    private String idempotencyKey;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
}
