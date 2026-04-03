package com.InvitationSystem.InvitationSystem.service.impl;

import com.InvitationSystem.InvitationSystem.Dto.deliveryLogsDto.DeliveryLogRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.deliveryLogsDto.DeliveryLogResponseDto;
import com.InvitationSystem.InvitationSystem.entity.DeliveryLog;
import com.InvitationSystem.InvitationSystem.repository.DeliveryLogRepository;
import com.InvitationSystem.InvitationSystem.service.DeliveryLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DeliveryLogServiceImpl implements DeliveryLogService {

    @Autowired
    private DeliveryLogRepository deliveryLogRepository;

    @Override
    public DeliveryLogResponseDto createDeliveryLog(DeliveryLogRequestDto request) {
        DeliveryLog log = DeliveryLog.builder()
                .invitationId(request.getInvitationId())
                .channel(request.getChannel())
                .status(request.getStatus())
                .retryCount(0)
                .sentAt(LocalDateTime.now())
                .build();

        DeliveryLog savedLog = deliveryLogRepository.save(log);
        return mapToResponseDto(savedLog);
    }

    @Override
    public DeliveryLogResponseDto getDeliveryLogById(UUID logId) {
        DeliveryLog log = deliveryLogRepository.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("Delivery log not found with ID: " + logId));
        return mapToResponseDto(log);
    }

    @Override
    public List<DeliveryLogResponseDto> getDeliveryLogsByInvitation(UUID invitationId) {
        return deliveryLogRepository.findByInvitationId(invitationId).stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public List<DeliveryLogResponseDto> getAllDeliveryLogs() {
        return deliveryLogRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public List<DeliveryLogResponseDto> getLogsByStatus(String status) {
        return deliveryLogRepository.findByStatusOrderBySentAtDesc(status).stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public List<DeliveryLogResponseDto> getLogsByChannel(String channel) {
        return deliveryLogRepository.findByChannel(channel).stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public DeliveryLogResponseDto markAsDelivered(UUID logId, String providerResponse) {
        DeliveryLog log = deliveryLogRepository.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("Delivery log not found with ID: " + logId));

        log.setStatus("DELIVERED");
        log.setDeliveredAt(LocalDateTime.now());
        log.setProviderResponse(providerResponse);

        DeliveryLog updatedLog = deliveryLogRepository.save(log);
        return mapToResponseDto(updatedLog);
    }

    @Override
    public DeliveryLogResponseDto markAsFailed(UUID logId, String providerResponse) {
        DeliveryLog log = deliveryLogRepository.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("Delivery log not found with ID: " + logId));

        log.setStatus("FAILED");
        log.setProviderResponse(providerResponse);
        log.setRetryCount(log.getRetryCount() + 1);

        DeliveryLog updatedLog = deliveryLogRepository.save(log);
        return mapToResponseDto(updatedLog);
    }

    @Override
    public void deleteDeliveryLog(UUID logId) {
        deliveryLogRepository.deleteById(logId);
    }

    private DeliveryLogResponseDto mapToResponseDto(DeliveryLog log) {
        DeliveryLogResponseDto dto = new DeliveryLogResponseDto();
        dto.setId(log.getId());
        dto.setInvitationId(log.getInvitationId());
        dto.setChannel(log.getChannel());
        dto.setStatus(log.getStatus());
        dto.setProviderResponse(log.getProviderResponse());
        dto.setRetryCount(log.getRetryCount());
        dto.setSentAt(log.getSentAt());
        dto.setDeliveredAt(log.getDeliveredAt());
        return dto;
    }
}
