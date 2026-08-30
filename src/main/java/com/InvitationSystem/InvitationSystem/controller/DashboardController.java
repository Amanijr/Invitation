package com.InvitationSystem.InvitationSystem.controller;

import com.InvitationSystem.InvitationSystem.Dto.dashboardDto.DashboardMetricsDto;
import com.InvitationSystem.InvitationSystem.entity.User;
import com.InvitationSystem.InvitationSystem.security.DeskUsers;
import com.InvitationSystem.InvitationSystem.service.DashboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Operational analytics and platform metrics endpoints")
public class DashboardController {

    private final DashboardService dashboardService;
    private final DeskUsers deskUsers;

    public DashboardController(DashboardService dashboardService, DeskUsers deskUsers) {
        this.dashboardService = dashboardService;
        this.deskUsers = deskUsers;
    }

    @GetMapping("/metrics")
    public ResponseEntity<DashboardMetricsDto> getDashboardMetrics(
            @RequestParam(required = false) UUID eventId,
            Authentication authentication) {
        User user = deskUsers.require(authentication);
        DashboardMetricsDto metrics = dashboardService.getDashboardMetrics(
                eventId, user.getUserId(), user.getRole());
        return ResponseEntity.ok(metrics);
    }
}
