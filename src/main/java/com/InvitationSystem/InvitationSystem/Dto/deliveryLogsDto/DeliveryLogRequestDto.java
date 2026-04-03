package com.InvitationSystem.InvitationSystem.Dto.deliveryLogsDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@io.swagger.v3.oas.annotations.media.Schema(description = "Request payload for creating a delivery log record")
public class DeliveryLogRequestDto {

    private UUID invitationId;
    private String channel;
    private String status;
}
