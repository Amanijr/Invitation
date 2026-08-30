package com.InvitationSystem.InvitationSystem.service.impl;

import com.InvitationSystem.InvitationSystem.Dto.dashboardDto.DashboardMetricsDto;
import com.InvitationSystem.InvitationSystem.entity.*;
import com.InvitationSystem.InvitationSystem.repository.*;
import com.InvitationSystem.InvitationSystem.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class DashboardServiceImplTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private DeliveryLogRepository deliveryLogRepository;

    @Autowired
    private CheckInRepository checkInRepository;

    private Event testEvent;

    @BeforeEach
    void setUp() {
        checkInRepository.deleteAllInBatch();
        deliveryLogRepository.deleteAllInBatch();
        invitationRepository.deleteAllInBatch();
        guestRepository.deleteAllInBatch();
        eventRepository.deleteAllInBatch();

        testEvent = eventRepository.save(Event.builder()
                .eventName("Dashboard Launch Gala")
                .venue("Main Hall")
                .eventDate(LocalDateTime.now().plusDays(5))
                .eventType(EventType.CORPORATE)
                .createdBy(UUID.randomUUID())
                .status("ACTIVE")
                .build());

        Guest g1 = guestRepository.save(Guest.builder()
                .fullName("Alice Wonder")
                .email("alice@example.com")
                .phone("+1112223333")
                .eventId(testEvent.getId())
                .build());

        Guest g2 = guestRepository.save(Guest.builder()
                .fullName("Bob Builder")
                .email("bob@example.com")
                .phone("+4445556666")
                .eventId(testEvent.getId())
                .build());

        UUID dummyTemplateId = UUID.randomUUID();

        Invitation inv1 = invitationRepository.save(Invitation.builder()
                .eventId(testEvent.getId())
                .guestId(g1.getId())
                .templateId(dummyTemplateId)
                .recipientEmail(g1.getEmail())
                .uniqueToken("TOK-DASH-1")
                .status(InvitationStatus.SENT)
                .used(true)
                .scanned(true)
                .build());

        Invitation inv2 = invitationRepository.save(Invitation.builder()
                .eventId(testEvent.getId())
                .guestId(g2.getId())
                .templateId(dummyTemplateId)
                .recipientEmail(g2.getEmail())
                .uniqueToken("TOK-DASH-2")
                .status(InvitationStatus.GENERATED)
                .used(false)
                .scanned(false)
                .build());

        deliveryLogRepository.save(DeliveryLog.builder()
                .invitationId(inv1.getId())
                .guestId(g1.getId())
                .channel("EMAIL")
                .status("DELIVERED")
                .build());

        deliveryLogRepository.save(DeliveryLog.builder()
                .invitationId(inv2.getId())
                .guestId(g2.getId())
                .channel("SMS")
                .status("FAILED")
                .errorMessage("Carrier network timeout")
                .build());
    }

    @Test
    @DisplayName("1. Calculate global platform metrics correctly from real database records")
    void testGetGlobalDashboardMetrics() {
        DashboardMetricsDto metrics = dashboardService.getDashboardMetrics(null);

        assertNotNull(metrics);
        assertEquals("All Platform Events", metrics.getEventName());
        assertEquals(1, metrics.getTotalEvents());
        assertEquals(2, metrics.getTotalGuests());
        assertEquals(2, metrics.getInvitationsGenerated());
        assertEquals(1, metrics.getVerifiedGuests());
        assertEquals(1, metrics.getUnverifiedGuests());
        assertEquals(1, metrics.getEmailDeliveries());
        assertEquals(1, metrics.getSmsDeliveries());
        assertEquals(1, metrics.getFailedDeliveries());
    }

    @Test
    @DisplayName("2. Calculate event-specific metrics correctly")
    void testGetEventSpecificDashboardMetrics() {
        DashboardMetricsDto metrics = dashboardService.getDashboardMetrics(testEvent.getId());

        assertNotNull(metrics);
        assertEquals(testEvent.getId(), metrics.getEventId());
        assertEquals("Dashboard Launch Gala", metrics.getEventName());
        assertEquals(1, metrics.getTotalEvents());
        assertEquals(2, metrics.getTotalGuests());
        assertEquals(2, metrics.getInvitationsGenerated());
        assertEquals(1, metrics.getVerifiedGuests());
        assertEquals(1, metrics.getUnverifiedGuests());
    }

    @Test
    @DisplayName("3. Event manager metrics exclude another creator's events")
    void testGetDashboardMetrics_scopedToCreator() {
        UUID otherCreator = UUID.randomUUID();
        eventRepository.save(Event.builder()
                .eventName("Other wedding")
                .venue("Other hall")
                .eventDate(LocalDateTime.now().plusDays(9))
                .eventType(EventType.WEDDING)
                .createdBy(otherCreator)
                .status("ACTIVE")
                .build());

        DashboardMetricsDto metrics = dashboardService.getDashboardMetrics(
                null, testEvent.getCreatedBy(), UserRole.EVENT_MANAGER);

        assertNotNull(metrics);
        assertEquals("Your events", metrics.getEventName());
        assertEquals(1, metrics.getTotalEvents());
        assertEquals(2, metrics.getTotalGuests());
    }
}
