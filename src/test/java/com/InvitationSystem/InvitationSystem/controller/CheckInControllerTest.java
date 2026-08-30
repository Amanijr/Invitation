package com.InvitationSystem.InvitationSystem.controller;

import com.InvitationSystem.InvitationSystem.Dto.checkInDto.CheckInRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.checkInDto.CheckInResponseDto;
import com.InvitationSystem.InvitationSystem.entity.CheckInResult;
import com.InvitationSystem.InvitationSystem.service.InvitationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CheckInControllerTest {

    @Mock
    private InvitationService invitationService;

    @InjectMocks
    private CheckInController checkInController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private UUID eventId;
    private String scannerId;
    private UUID invitationId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(checkInController).build();
        objectMapper = new ObjectMapper();

        eventId = UUID.randomUUID();
        scannerId = "Main entrance";
        invitationId = UUID.randomUUID();
    }

    @Test
    @DisplayName("1. Successful verification returns 200 OK with CheckInResult.SUCCESS")
    void testVerifyInvitation_success() throws Exception {
        CheckInRequestDto request = CheckInRequestDto.builder()
                .token("VALID-TOKEN-123")
                .eventId(eventId)
                .scannerId(scannerId)
                .build();

        CheckInResponseDto response = CheckInResponseDto.builder()
                .checkInId(UUID.randomUUID())
                .invitationId(invitationId)
                .eventId(eventId)
                .token("VALID-TOKEN-123")
                .result(CheckInResult.SUCCESS)
                .message("Check-in successful")
                .guestName("John Doe")
                .eventName("Gala Event")
                .scannedAt(LocalDateTime.now())
                .scannerId(scannerId)
                .build();

        when(invitationService.verifyInvitation(any(CheckInRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/check-in/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Check-in successful"))
                .andExpect(jsonPath("$.guestName").value("John Doe"))
                .andExpect(jsonPath("$.eventName").value("Gala Event"));
    }

    @Test
    @DisplayName("2. Duplicate scan returns 200 OK with CheckInResult.ALREADY_USED")
    void testVerifyInvitation_alreadyUsed() throws Exception {
        CheckInRequestDto request = CheckInRequestDto.builder()
                .token("USED-TOKEN-123")
                .eventId(eventId)
                .scannerId(scannerId)
                .build();

        CheckInResponseDto response = CheckInResponseDto.builder()
                .checkInId(UUID.randomUUID())
                .invitationId(invitationId)
                .eventId(eventId)
                .token("USED-TOKEN-123")
                .result(CheckInResult.ALREADY_USED)
                .message("Already checked in")
                .guestName("John Doe")
                .eventName("Gala Event")
                .scannedAt(LocalDateTime.now())
                .scannerId(scannerId)
                .build();

        when(invitationService.verifyInvitation(any(CheckInRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/check-in/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("ALREADY_USED"))
                .andExpect(jsonPath("$.message").value("Already checked in"));
    }

    @Test
    @DisplayName("3. Invalid token returns 200 OK with CheckInResult.INVALID_TOKEN")
    void testVerifyInvitation_invalidToken() throws Exception {
        CheckInRequestDto request = CheckInRequestDto.builder()
                .token("NON-EXISTENT-TOKEN")
                .eventId(eventId)
                .scannerId(scannerId)
                .build();

        CheckInResponseDto response = CheckInResponseDto.builder()
                .token("NON-EXISTENT-TOKEN")
                .result(CheckInResult.INVALID_TOKEN)
                .message("Invalid invitation token")
                .build();

        when(invitationService.verifyInvitation(any(CheckInRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/check-in/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("INVALID_TOKEN"))
                .andExpect(jsonPath("$.message").value("Invalid invitation token"));
    }

    @Test
    @DisplayName("4. Expired token returns 200 OK with CheckInResult.EXPIRED")
    void testVerifyInvitation_expired() throws Exception {
        CheckInRequestDto request = CheckInRequestDto.builder()
                .token("EXPIRED-TOKEN-123")
                .eventId(eventId)
                .scannerId(scannerId)
                .build();

        CheckInResponseDto response = CheckInResponseDto.builder()
                .token("EXPIRED-TOKEN-123")
                .result(CheckInResult.EXPIRED)
                .message("Invitation has expired")
                .build();

        when(invitationService.verifyInvitation(any(CheckInRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/check-in/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("EXPIRED"))
                .andExpect(jsonPath("$.message").value("Invitation has expired"));
    }

    @Test
    @DisplayName("5. Event mismatch returns 200 OK with CheckInResult.EVENT_MISMATCH")
    void testVerifyInvitation_eventMismatch() throws Exception {
        CheckInRequestDto request = CheckInRequestDto.builder()
                .token("TOKEN-FOR-OTHER-EVENT")
                .eventId(eventId)
                .scannerId(scannerId)
                .build();

        CheckInResponseDto response = CheckInResponseDto.builder()
                .token("TOKEN-FOR-OTHER-EVENT")
                .result(CheckInResult.EVENT_MISMATCH)
                .message("Invitation does not belong to this event")
                .build();

        when(invitationService.verifyInvitation(any(CheckInRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/check-in/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("EVENT_MISMATCH"))
                .andExpect(jsonPath("$.message").value("Invitation does not belong to this event"));
    }

    @Test
    @DisplayName("6. Unauthorized scanner returns 200 OK with CheckInResult.UNAUTHORIZED")
    void testVerifyInvitation_unauthorizedScanner() throws Exception {
        CheckInRequestDto request = CheckInRequestDto.builder()
                .token("SOME-TOKEN")
                .eventId(eventId)
                .scannerId(scannerId)
                .build();

        CheckInResponseDto response = CheckInResponseDto.builder()
                .token("SOME-TOKEN")
                .result(CheckInResult.UNAUTHORIZED)
                .message("Scanner is unauthorized")
                .build();

        when(invitationService.verifyInvitation(any(CheckInRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/check-in/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Scanner is unauthorized"));
    }

    @Test
    @DisplayName("7. Blank token payload returns 400 Bad Request")
    void testVerifyInvitation_blankToken_badRequest() throws Exception {
        CheckInRequestDto request = CheckInRequestDto.builder()
                .token("   ")
                .eventId(eventId)
                .build();

        mockMvc.perform(post("/api/v1/check-in/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
