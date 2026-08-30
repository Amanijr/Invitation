package com.InvitationSystem.InvitationSystem.controller;

import com.InvitationSystem.InvitationSystem.Dto.rsvpDto.RsvpGuestViewDto;
import com.InvitationSystem.InvitationSystem.Dto.rsvpDto.RsvpSubmitRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.rsvpDto.RsvpTrackingDto;
import com.InvitationSystem.InvitationSystem.entity.RsvpStatus;
import com.InvitationSystem.InvitationSystem.service.RsvpService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RsvpControllerTest {

    @Mock
    private RsvpService rsvpService;

    @InjectMocks
    private RsvpController rsvpController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(rsvpController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void openInvitation_ReturnsGuestView() throws Exception {
        when(rsvpService.openInvitation("abc123")).thenReturn(RsvpGuestViewDto.builder()
                .token("abc123")
                .guestName("John Mwita")
                .eventName("Amani & Neema")
                .venue("The Slipway, Dar es Salaam")
                .askDietary(true)
                .askMeal(true)
                .mealOptions(List.of("Chicken", "Steak"))
                .rsvpStatus(RsvpStatus.NO_REPLY)
                .partySize(1)
                .checkedIn(false)
                .build());

        mockMvc.perform(get("/api/v1/rsvp/{token}", "abc123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guestName").value("John Mwita"))
                .andExpect(jsonPath("$.rsvpStatus").value("NO_REPLY"));
    }

    @Test
    void submitRsvp_ReturnsUpdatedView() throws Exception {
        RsvpSubmitRequestDto request = RsvpSubmitRequestDto.builder()
                .status(RsvpStatus.GOING)
                .partySize(2)
                .mealChoice("Chicken")
                .build();

        when(rsvpService.submitRsvp(eq("abc123"), any(RsvpSubmitRequestDto.class)))
                .thenReturn(RsvpGuestViewDto.builder()
                        .token("abc123")
                        .guestName("John Mwita")
                        .rsvpStatus(RsvpStatus.GOING)
                        .partySize(2)
                        .mealChoice("Chicken")
                        .build());

        mockMvc.perform(post("/api/v1/rsvp/{token}", "abc123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rsvpStatus").value("GOING"))
                .andExpect(jsonPath("$.partySize").value(2));
    }

    @Test
    void getTracking_ReturnsCounts() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(rsvpService.getTracking(eventId)).thenReturn(RsvpTrackingDto.builder()
                .total(3)
                .opened(2)
                .going(1)
                .notGoing(0)
                .maybe(1)
                .noReply(1)
                .guests(List.of())
                .activity(List.of())
                .build());

        mockMvc.perform(get("/api/v1/rsvp/event/{eventId}", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.going").value(1))
                .andExpect(jsonPath("$.maybe").value(1));
    }
}
