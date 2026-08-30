package com.InvitationSystem.InvitationSystem.controller;

import com.InvitationSystem.InvitationSystem.Dto.dashboardDto.DashboardMetricsDto;
import com.InvitationSystem.InvitationSystem.entity.User;
import com.InvitationSystem.InvitationSystem.entity.UserRole;
import com.InvitationSystem.InvitationSystem.security.DeskUsers;
import com.InvitationSystem.InvitationSystem.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    @Mock
    private DeskUsers deskUsers;

    @InjectMocks
    private DashboardController dashboardController;

    private MockMvc mockMvc;
    private User deskUser;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(dashboardController).build();
        deskUser = User.builder()
                .userId(UUID.randomUUID())
                .firstName("Amani")
                .lastName("Juma")
                .email("amani@studio.com")
                .passwordHash("hash")
                .role(UserRole.EVENT_MANAGER)
                .build();
        authentication = new UsernamePasswordAuthenticationToken(deskUser.getEmail(), "n");
        when(deskUsers.require(any(Authentication.class))).thenReturn(deskUser);
    }

    @Test
    @DisplayName("1. GET /api/v1/dashboard/metrics returns the signed-in desk metrics")
    void testGetDashboardMetrics_global() throws Exception {
        DashboardMetricsDto dto = DashboardMetricsDto.builder()
                .eventName("Your events")
                .totalEvents(5)
                .totalGuests(100)
                .invitationsGenerated(90)
                .invitationsSent(80)
                .emailDeliveries(40)
                .smsDeliveries(30)
                .whatsAppDeliveries(20)
                .verifiedGuests(50)
                .unverifiedGuests(50)
                .failedDeliveries(5)
                .build();

        when(dashboardService.getDashboardMetrics(isNull(), eq(deskUser.getUserId()), eq(UserRole.EVENT_MANAGER)))
                .thenReturn(dto);

        mockMvc.perform(get("/api/v1/dashboard/metrics").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventName").value("Your events"))
                .andExpect(jsonPath("$.totalEvents").value(5))
                .andExpect(jsonPath("$.totalGuests").value(100))
                .andExpect(jsonPath("$.verifiedGuests").value(50));
    }

    @Test
    @DisplayName("2. GET /api/v1/dashboard/metrics?eventId=... returns event-specific metrics")
    void testGetDashboardMetrics_eventSpecific() throws Exception {
        UUID eventId = UUID.randomUUID();
        DashboardMetricsDto dto = DashboardMetricsDto.builder()
                .eventId(eventId)
                .eventName("Tech Summit 2026")
                .totalEvents(1)
                .totalGuests(30)
                .invitationsGenerated(30)
                .invitationsSent(25)
                .verifiedGuests(15)
                .unverifiedGuests(15)
                .failedDeliveries(1)
                .build();

        when(dashboardService.getDashboardMetrics(eq(eventId), eq(deskUser.getUserId()), eq(UserRole.EVENT_MANAGER)))
                .thenReturn(dto);

        mockMvc.perform(get("/api/v1/dashboard/metrics")
                        .param("eventId", eventId.toString())
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.eventName").value("Tech Summit 2026"))
                .andExpect(jsonPath("$.totalGuests").value(30));
    }
}
