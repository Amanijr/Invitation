package com.InvitationSystem.InvitationSystem.service.impl;

import com.InvitationSystem.InvitationSystem.Dto.dashboardDto.DashboardMetricsDto;
import com.InvitationSystem.InvitationSystem.entity.Event;
import com.InvitationSystem.InvitationSystem.entity.Invitation;
import com.InvitationSystem.InvitationSystem.entity.InvitationStatus;
import com.InvitationSystem.InvitationSystem.entity.UserRole;
import com.InvitationSystem.InvitationSystem.repository.DeliveryLogRepository;
import com.InvitationSystem.InvitationSystem.repository.EventRepository;
import com.InvitationSystem.InvitationSystem.repository.GuestRepository;
import com.InvitationSystem.InvitationSystem.repository.InvitationRepository;
import com.InvitationSystem.InvitationSystem.security.EventAuthorization;
import com.InvitationSystem.InvitationSystem.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private DeliveryLogRepository deliveryLogRepository;

    @Override
    public DashboardMetricsDto getDashboardMetrics(UUID eventId) {
        return getDashboardMetrics(eventId, null, UserRole.ADMIN);
    }

    @Override
    public DashboardMetricsDto getDashboardMetrics(UUID eventId, UUID actorId, UserRole actorRole) {
        UserRole role = actorRole == null ? UserRole.ADMIN : actorRole;
        if (eventId != null) {
            Event event = eventRepository.findById(eventId).orElse(null);
            if (event == null) {
                return emptyMetrics(eventId, "Unknown Event");
            }
            if (actorId != null) {
                EventAuthorization.requireEventOwnerOrAdmin(event, actorId, role);
            }
            return metricsForInvitations(
                    List.of(eventId),
                    eventId,
                    event.getEventName(),
                    1);
        }

        List<UUID> eventIds = role == UserRole.ADMIN
                ? eventRepository.findAll().stream().map(Event::getId).toList()
                : eventRepository.findByCreatedBy(actorId).stream().map(Event::getId).toList();
        String name = role == UserRole.ADMIN ? "All Platform Events" : "Your events";
        return metricsForInvitations(eventIds, null, name, eventIds.size());
    }

    private DashboardMetricsDto emptyMetrics(UUID eventId, String eventName) {
        return DashboardMetricsDto.builder()
                .eventId(eventId)
                .eventName(eventName)
                .totalEvents(0)
                .totalGuests(0)
                .invitationsGenerated(0)
                .invitationsSent(0)
                .emailDeliveries(0)
                .smsDeliveries(0)
                .whatsAppDeliveries(0)
                .verifiedGuests(0)
                .unverifiedGuests(0)
                .failedDeliveries(0)
                .build();
    }

    private DashboardMetricsDto metricsForInvitations(
            List<UUID> eventIds,
            UUID eventId,
            String eventName,
            long totalEvents) {
        if (eventIds.isEmpty()) {
            return emptyMetrics(eventId, eventName);
        }

        long totalGuests = guestRepository.countByEventIdIn(eventIds);
        long invitationsGenerated = invitationRepository.countByEventIdIn(eventIds);
        long verifiedGuests = invitationRepository.countByEventIdInAndUsedTrue(eventIds);
        long unverifiedGuests = Math.max(0, totalGuests - verifiedGuests);

        List<Invitation> invitations = invitationRepository.findByEventIdIn(eventIds);
        long invitationsSent = invitations.stream()
                .filter(i -> i.getStatus() == InvitationStatus.SENT || i.isUsed() || i.isScanned())
                .count();

        long emailDeliveries = 0;
        long smsDeliveries = 0;
        long whatsAppDeliveries = 0;
        long failedDeliveries = 0;

        for (Invitation inv : invitations) {
            var logs = deliveryLogRepository.findByInvitationId(inv.getId());
            for (var log : logs) {
                if ("EMAIL".equalsIgnoreCase(log.getChannel())) emailDeliveries++;
                else if ("SMS".equalsIgnoreCase(log.getChannel())) smsDeliveries++;
                else if ("WHATSAPP".equalsIgnoreCase(log.getChannel())) whatsAppDeliveries++;

                if ("FAILED".equalsIgnoreCase(log.getStatus())) failedDeliveries++;
            }
        }

        return DashboardMetricsDto.builder()
                .eventId(eventId)
                .eventName(eventName)
                .totalEvents(totalEvents)
                .totalGuests(totalGuests)
                .invitationsGenerated(invitationsGenerated)
                .invitationsSent(invitationsSent)
                .emailDeliveries(emailDeliveries)
                .smsDeliveries(smsDeliveries)
                .whatsAppDeliveries(whatsAppDeliveries)
                .verifiedGuests(verifiedGuests)
                .unverifiedGuests(unverifiedGuests)
                .failedDeliveries(failedDeliveries)
                .build();
    }
}
