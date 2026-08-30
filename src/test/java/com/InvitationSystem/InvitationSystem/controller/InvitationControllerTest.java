package com.InvitationSystem.InvitationSystem.controller;

import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationDetailedResponseDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationResponseDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationScanResponseDto;
import com.InvitationSystem.InvitationSystem.entity.InvitationStatus;
import com.InvitationSystem.InvitationSystem.entity.User;
import com.InvitationSystem.InvitationSystem.entity.UserRole;
import com.InvitationSystem.InvitationSystem.security.DeskUsers;
import com.InvitationSystem.InvitationSystem.service.ECardRenderingEngineService;
import com.InvitationSystem.InvitationSystem.service.EventService;
import com.InvitationSystem.InvitationSystem.service.InvitationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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

    @Mock
    private ECardRenderingEngineService eCardRenderingEngineService;

    @Mock
    private DeskUsers deskUsers;

    @Mock
    private EventService eventService;

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
        responseDto.setUsed(true);

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
                .andExpect(jsonPath("$.id").value(invitationId.toString()))
                .andExpect(jsonPath("$.used").value(true));
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
    void getInvitationCardByToken_returnsPng() throws Exception {
        when(eCardRenderingEngineService.renderCardImageBytesByToken("tok-1")).thenReturn(new byte[] {1, 2, 3});

        mockMvc.perform(get("/api/v1/invitations/token/{token}/card", "tok-1"))
                .andExpect(status().isOk());
    }

    @Test
    void getInvitationCardByToken_notFound() throws Exception {
        when(eCardRenderingEngineService.renderCardImageBytesByToken("missing"))
                .thenThrow(new IllegalArgumentException("Invitation not found for token"));

        mockMvc.perform(get("/api/v1/invitations/token/{token}/card", "missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getInvitationByEvent_success() throws Exception {
        User deskUser = User.builder()
                .userId(UUID.randomUUID())
                .firstName("Amani")
                .lastName("Juma")
                .email("amani@studio.com")
                .passwordHash("hash")
                .role(UserRole.EVENT_MANAGER)
                .build();
        when(deskUsers.require(any(Authentication.class))).thenReturn(deskUser);
        doNothing().when(eventService).assertCanAccess(eventId, deskUser.getUserId(), deskUser.getRole());
        when(invitationService.getInvitationsByEvent(eventId)).thenReturn(List.of(detailedDto));

        mockMvc.perform(get("/api/v1/invitations/event/{eventId}", eventId)
                        .principal(new UsernamePasswordAuthenticationToken(deskUser.getEmail(), "n")))
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
                InvitationStatus.SENT,
                false,
                null,
                "Invitation valid"
        );
        when(invitationService.scanInvitationByToken("token-1")).thenReturn(scanResponseDto);

        mockMvc.perform(get("/api/v1/invitations/scan/{token}", "token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Invitation valid"));
    }

    @Test
    void deleteInvitation_success() throws Exception {
        doNothing().when(invitationService).deleteInvitation(invitationId);

        mockMvc.perform(delete("/api/v1/invitations/{invitationId}", invitationId))
                .andExpect(status().isNoContent());

        verify(invitationService, times(1)).deleteInvitation(invitationId);
    }

    @Test
    void generateBulkInvitations_success() throws Exception {
        UUID templateId = UUID.randomUUID();
        com.InvitationSystem.InvitationSystem.Dto.invitationsDto.BulkGenerationRequestDto bulkReq =
                com.InvitationSystem.InvitationSystem.Dto.invitationsDto.BulkGenerationRequestDto.builder()
                        .eventId(eventId)
                        .templateId(templateId)
                        .regenerationPolicy(com.InvitationSystem.InvitationSystem.Dto.invitationsDto.RegenerationPolicy.SKIP_EXISTING)
                        .build();

        com.InvitationSystem.InvitationSystem.Dto.invitationsDto.BulkGenerationResultDto bulkResult =
                com.InvitationSystem.InvitationSystem.Dto.invitationsDto.BulkGenerationResultDto.builder()
                        .eventId(eventId)
                        .templateId(templateId)
                        .totalGuests(5)
                        .successCount(4)
                        .skippedCount(1)
                        .failedCount(0)
                        .successfulInvitationIds(List.of(UUID.randomUUID(), UUID.randomUUID()))
                        .errors(List.of())
                        .build();

        when(invitationService.generateBulkInvitations(any(com.InvitationSystem.InvitationSystem.Dto.invitationsDto.BulkGenerationRequestDto.class)))
                .thenReturn(bulkResult);

        mockMvc.perform(post("/api/v1/invitations/generate-bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulkReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalGuests").value(5))
                .andExpect(jsonPath("$.successCount").value(4))
                .andExpect(jsonPath("$.skippedCount").value(1));
    }
}
