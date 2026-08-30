package com.InvitationSystem.InvitationSystem.service;

import com.InvitationSystem.InvitationSystem.Dto.dashboardDto.DashboardMetricsDto;
import com.InvitationSystem.InvitationSystem.entity.UserRole;

import java.util.UUID;

public interface DashboardService {
    DashboardMetricsDto getDashboardMetrics(UUID eventId);

    DashboardMetricsDto getDashboardMetrics(UUID eventId, UUID actorId, UserRole actorRole);
}
