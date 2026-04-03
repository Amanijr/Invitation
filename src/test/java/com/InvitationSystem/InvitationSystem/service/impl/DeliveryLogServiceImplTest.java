package com.InvitationSystem.InvitationSystem.service.impl;

import com.InvitationSystem.InvitationSystem.Dto.deliveryLogsDto.DeliveryLogRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.deliveryLogsDto.DeliveryLogResponseDto;
import com.InvitationSystem.InvitationSystem.entity.DeliveryLog;
import com.InvitationSystem.InvitationSystem.repository.DeliveryLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryLogServiceImplTest {

    @Mock
    private DeliveryLogRepository deliveryLogRepository;

    @InjectMocks
    private DeliveryLogServiceImpl deliveryLogService;

    private UUID logId;
    private UUID invitationId;

    @BeforeEach
    void setUp() {
        logId = UUID.randomUUID();
        invitationId = UUID.randomUUID();
    }

    @Test
    void createDeliveryLog_success() {
        DeliveryLogRequestDto request = new DeliveryLogRequestDto(invitationId, "EMAIL", "SENT");
        when(deliveryLogRepository.save(any(DeliveryLog.class))).thenAnswer(invocation -> {
            DeliveryLog log = invocation.getArgument(0);
            log.setId(logId);
            return log;
        });

        DeliveryLogResponseDto response = deliveryLogService.createDeliveryLog(request);

        assertEquals(logId, response.getId());
        assertEquals("EMAIL", response.getChannel());
    }

    @Test
    void markAsDelivered_notFound_throws() {
        when(deliveryLogRepository.findById(logId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> deliveryLogService.markAsDelivered(logId, "OK"));
    }

    @Test
    void markAsFailed_incrementsRetry() {
        DeliveryLog log = DeliveryLog.builder().id(logId).invitationId(invitationId).retryCount(1).status("SENT").build();
        when(deliveryLogRepository.findById(logId)).thenReturn(Optional.of(log));
        when(deliveryLogRepository.save(any(DeliveryLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryLogResponseDto response = deliveryLogService.markAsFailed(logId, "Timeout");

        assertEquals("FAILED", response.getStatus());
        assertEquals(2, response.getRetryCount());
    }
}
