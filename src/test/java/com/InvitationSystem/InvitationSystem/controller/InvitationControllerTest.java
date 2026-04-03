package com.InvitationSystem.InvitationSystem.controller;

import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationDetailedResponseDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationResponseDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationScanResponseDto;
import com.InvitationSystem.InvitationSystem.entity.InvitationStatus;
import com.InvitationSystem.InvitationSystem.service.InvitationService;
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

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InvitationControllerTest {

    @Mock
    private InvitationService invitationService;

    @InjectMocks
    private InvitationController invitationController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private UUID invitationId;
    private UUID eventId;
    private UUID guestId;
    private InvitationResponseDto responseDto;
    private InvitationDetailedResponseDto detailedDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(invitationController).build();
        objectMapper = new ObjectMapper();

        invitationId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        guestId = UUID.randomUUID();

        responseDto = new InvitationResponseDto();
        responseDto.setId(invitationId);
        responseDto.setStatus(InvitationStatus.GENERATED);

        detailedDto = new InvitationDetailedResponseDto();
        detailedDto.setId(invitationId);
        detailedDto.setStatus(InvitationStatus.SENT);
        detailedDto.setRecipientEmail("guest@example.com");
    }

    @Test
    void createInvitation_success() throws Exception {
        InvitationRequestDto requestDto = new InvitationRequestDto();
        requestDto.setEventId(eventId);
        requestDto.setGuestId(guestId);
        requestDto.setTemplateId(UUID.randomUUID());
        requestDto.setRecipientEmail("guest@example.com");

        when(invitationService.createInvitation(any(InvitationRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/invitations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(invitationId.toString()));
    }

    @Test
    void getInvitationByToken_forbiddenOnValidationFailure() throws Exception {
        String token = "bad-token";
        doThrow(new IllegalArgumentException("Invalid invitation token"))
                .when(invitationService).validateInvitation(token, null, null);

        mockMvc.perform(get("/api/v1/invitations/token/{token}", token))
                .andExpect(status().isForbidden());
    }

    @Test
    void getInvitationByEvent_success() throws Exception {
        when(invitationService.getInvitationsByEvent(eventId)).thenReturn(List.of(detailedDto));

        mockMvc.perform(get("/api/v1/invitations/event/{eventId}", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getInvitationByStatus_success() throws Exception {
        when(invitationService.getInvitationsByStatus(InvitationStatus.SENT)).thenReturn(List.of(detailedDto));

        mockMvc.perform(get("/api/v1/invitations/status/{status}", "sent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void markAsSent_success() throws Exception {
        when(invitationService.markAsSent(invitationId)).thenReturn(detailedDto);

        mockMvc.perform(patch("/api/v1/invitations/{invitationId}/mark-sent", invitationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invitationId.toString()));

        verify(invitationService, times(1)).markAsSent(invitationId);
    }

    @Test
    void scanInvitation_success() throws Exception {
        InvitationScanResponseDto scanResponseDto = new InvitationScanResponseDto(
                invitationId,
                "token-1",
                InvitationStatus.USED,
                true,
                null,
                "Check-in successful"
        );
        when(invitationService.scanInvitationByToken("token-1")).thenReturn(scanResponseDto);

        mockMvc.perform(get("/api/v1/invitations/scan/{token}", "token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Check-in successful"));
    }

    @Test
    void deleteInvitation_success() throws Exception {
        doNothing().when(invitationService).deleteInvitation(invitationId);

        mockMvc.perform(delete("/api/v1/invitations/{invitationId}", invitationId))
                .andExpect(status().isNoContent());

        verify(invitationService, times(1)).deleteInvitation(invitationId);
    }
}
