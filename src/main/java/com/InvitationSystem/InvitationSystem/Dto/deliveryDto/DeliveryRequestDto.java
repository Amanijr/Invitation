package com.InvitationSystem.InvitationSystem.Dto.deliveryDto;

import com.InvitationSystem.InvitationSystem.entity.DeliveryChannel;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
@io.swagger.v3.oas.annotations.media.Schema(description = "Request payload for sending an invitation across one or more delivery channels")
public class DeliveryRequestDto {

    @NotNull(message = "invitationId is required")
    private UUID invitationId;

    @NotEmpty(message = "channels list cannot be empty")
    private List<DeliveryChannel> channels;

    private String recipientEmail;

    private String recipientPhone;

    private String idempotencyKey;
}
