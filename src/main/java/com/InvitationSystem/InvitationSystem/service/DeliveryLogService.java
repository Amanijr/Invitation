package com.InvitationSystem.InvitationSystem.service;

import com.InvitationSystem.InvitationSystem.Dto.deliveryLogsDto.DeliveryLogRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.deliveryLogsDto.DeliveryLogResponseDto;

import java.util.List;
import java.util.UUID;

public interface DeliveryLogService {

    DeliveryLogResponseDto createDeliveryLog(DeliveryLogRequestDto request);

    DeliveryLogResponseDto getDeliveryLogById(UUID logId);

    List<DeliveryLogResponseDto> getDeliveryLogsByInvitation(UUID invitationId);

    List<DeliveryLogResponseDto> getAllDeliveryLogs();

    List<DeliveryLogResponseDto> getLogsByStatus(String status);

    List<DeliveryLogResponseDto> getLogsByChannel(String channel);

    DeliveryLogResponseDto markAsDelivered(UUID logId, String providerResponse);

    DeliveryLogResponseDto markAsFailed(UUID logId, String providerResponse);

    void deleteDeliveryLog(UUID logId);
}
