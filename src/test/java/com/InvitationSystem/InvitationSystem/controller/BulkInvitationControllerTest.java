package com.InvitationSystem.InvitationSystem.controller;

import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.BulkGenerationRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.BulkGenerationResultDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.RegenerationPolicy;
import com.InvitationSystem.InvitationSystem.service.ECardRenderingEngineService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BulkInvitationControllerTest {

    @Mock
    private InvitationService invitationService;

    @Mock
    private ECardRenderingEngineService eCardRenderingEngineService;

    @InjectMocks
    private InvitationController invitationController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private UUID eventId;
    private UUID templateId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(invitationController).build();
        objectMapper = new ObjectMapper();

        eventId = UUID.randomUUID();
        templateId = UUID.randomUUID();
    }

    @Test
    void generateBulkInvitations_ValidRequest_Returns201Created() throws Exception {
        BulkGenerationRequestDto request = BulkGenerationRequestDto.builder()
                .eventId(eventId)
                .templateId(templateId)
                .regenerationPolicy(RegenerationPolicy.SKIP_EXISTING)
                .build();

        BulkGenerationResultDto result = BulkGenerationResultDto.builder()
                .eventId(eventId)
                .templateId(templateId)
                .totalGuests(10)
                .successCount(8)
                .skippedCount(2)
                .failedCount(0)
                .successfulInvitationIds(List.of())
                .errors(List.of())
                .processedAt(LocalDateTime.now())
                .build();

        when(invitationService.generateBulkInvitations(any())).thenReturn(result);

        mockMvc.perform(post("/api/v1/invitations/generate-bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalGuests").value(10))
                .andExpect(jsonPath("$.successCount").value(8))
                .andExpect(jsonPath("$.skippedCount").value(2));
    }
}
