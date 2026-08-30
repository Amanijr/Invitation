package com.InvitationSystem.InvitationSystem.service;

import com.InvitationSystem.InvitationSystem.Dto.deliveryDto.*;
import com.InvitationSystem.InvitationSystem.Dto.deliveryLogsDto.DeliveryLogResponseDto;

import java.util.List;
import java.util.UUID;

public interface MultiChannelDeliveryService {

    MultiChannelDeliveryResponseDto sendInvitation(DeliveryRequestDto request);

    BatchDeliveryResponseDto sendBatchInvitations(BatchDeliveryRequestDto request);

    DeliveryLogResponseDto retryDelivery(UUID logId);

    List<DeliveryLogResponseDto> getLogsByInvitation(UUID invitationId);

    List<DeliveryLogResponseDto> getAllLogs();

    List<DeliveryLogResponseDto> getLogsForActor(UUID actorId, com.InvitationSystem.InvitationSystem.entity.UserRole role);
}
