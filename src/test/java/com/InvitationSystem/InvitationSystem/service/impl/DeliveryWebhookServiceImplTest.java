package com.InvitationSystem.InvitationSystem.service.impl;

import com.InvitationSystem.InvitationSystem.entity.DeliveryLog;
import com.InvitationSystem.InvitationSystem.repository.DeliveryLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryWebhookServiceImplTest {

    @Mock
    private DeliveryLogRepository deliveryLogRepository;

    @InjectMocks
    private DeliveryWebhookServiceImpl deliveryWebhookService;

    private DeliveryLog log;

    @BeforeEach
    void setUp() {
        log = DeliveryLog.builder()
                .id(UUID.randomUUID())
                .invitationId(UUID.randomUUID())
                .channel("SMS")
                .status("SENT")
                .providerReference("zXDYfTmTVf3iMd16zzdBj")
                .build();
    }

    @Test
    void smsDeliveredWebhookMarksDelivered() {
        when(deliveryLogRepository.findFirstByProviderReferenceOrderBySentAtDesc("zXDYfTmTVf3iMd16zzdBj"))
                .thenReturn(Optional.of(log));

        deliveryWebhookService.applySmsGateEvent(Map.of(
                "event", "sms:delivered",
                "payload", Map.of("messageId", "zXDYfTmTVf3iMd16zzdBj", "state", "Delivered")
        ));

        ArgumentCaptor<DeliveryLog> captor = ArgumentCaptor.forClass(DeliveryLog.class);
        verify(deliveryLogRepository).save(captor.capture());
        assertEquals("DELIVERED", captor.getValue().getStatus());
        assertNotNull(captor.getValue().getDeliveredAt());
    }

    @Test
    void smsSentWebhookDoesNotMarkDelivered() {
        when(deliveryLogRepository.findFirstByProviderReferenceOrderBySentAtDesc("zXDYfTmTVf3iMd16zzdBj"))
                .thenReturn(Optional.of(log));

        deliveryWebhookService.applySmsGateEvent(Map.of(
                "event", "sms:sent",
                "payload", Map.of("messageId", "zXDYfTmTVf3iMd16zzdBj", "state", "Sent")
        ));

        ArgumentCaptor<DeliveryLog> captor = ArgumentCaptor.forClass(DeliveryLog.class);
        verify(deliveryLogRepository).save(captor.capture());
        assertEquals("SENT", captor.getValue().getStatus());
        assertNull(captor.getValue().getDeliveredAt());
    }

    @Test
    void unknownMessageIdIsIgnored() {
        when(deliveryLogRepository.findFirstByProviderReferenceOrderBySentAtDesc("missing"))
                .thenReturn(Optional.empty());

        deliveryWebhookService.applySmsGateEvent(Map.of(
                "event", "sms:delivered",
                "payload", Map.of("messageId", "missing")
        ));

        verify(deliveryLogRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void evolutionDeliveryAckMarksDelivered() {
        when(deliveryLogRepository.findFirstByProviderReferenceOrderBySentAtDesc("BAE594145F4C59B4"))
                .thenReturn(Optional.of(log));
        log.setChannel("WHATSAPP");
        log.setProviderReference("BAE594145F4C59B4");

        deliveryWebhookService.applyEvolutionEvent(Map.of(
                "event", "messages.update",
                "data", Map.of("id", "BAE594145F4C59B4", "status", "DELIVERY_ACK")
        ));

        ArgumentCaptor<DeliveryLog> captor = ArgumentCaptor.forClass(DeliveryLog.class);
        verify(deliveryLogRepository).save(captor.capture());
        assertEquals("DELIVERED", captor.getValue().getStatus());
        assertNotNull(captor.getValue().getDeliveredAt());
    }

    @Test
    void evolutionPendingDoesNotMarkDelivered() {
        when(deliveryLogRepository.findFirstByProviderReferenceOrderBySentAtDesc("BAE594145F4C59B4"))
                .thenReturn(Optional.of(log));
        log.setProviderReference("BAE594145F4C59B4");

        deliveryWebhookService.applyEvolutionEvent(Map.of(
                "event", "messages.update",
                "data", Map.of("id", "BAE594145F4C59B4", "status", "PENDING")
        ));

        ArgumentCaptor<DeliveryLog> captor = ArgumentCaptor.forClass(DeliveryLog.class);
        verify(deliveryLogRepository).save(captor.capture());
        assertEquals("SENT", captor.getValue().getStatus());
        assertNull(captor.getValue().getDeliveredAt());
    }

    @Test
    void evolutionUpsertIsIgnored() {
        deliveryWebhookService.applyEvolutionEvent(Map.of(
                "event", "MESSAGES_UPSERT",
                "data", Map.of("id", "BAE594145F4C59B4", "status", "DELIVERY_ACK")
        ));

        verify(deliveryLogRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
