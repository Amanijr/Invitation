package com.InvitationSystem.InvitationSystem.Dto.dashboardDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardMetricsDto {

    private UUID eventId; // Null if global metrics across all events
    private String eventName; // Null if global metrics

    private long totalEvents;
    private long totalGuests;
    private long invitationsGenerated;
    private long invitationsSent;
    
    private long emailDeliveries;
    private long smsDeliveries;
    private long whatsAppDeliveries;

    private long verifiedGuests;
    private long unverifiedGuests;
    private long failedDeliveries;
}
