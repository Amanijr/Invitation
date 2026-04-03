package com.InvitationSystem.InvitationSystem.controller;

import com.InvitationSystem.InvitationSystem.Dto.bulkUploadDto.BulkUploadResponseDto;
import com.InvitationSystem.InvitationSystem.Dto.bulkUploadDto.BulkUploadSessionDto;
import com.InvitationSystem.InvitationSystem.entity.UploadStatus;
import com.InvitationSystem.InvitationSystem.exception.GlobalExceptionHandler;
import com.InvitationSystem.InvitationSystem.service.BulkUploadService;
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
import java.util.Arrays;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BulkUploadControllerTest {

    @Mock
    private BulkUploadService bulkUploadService;

    @InjectMocks
    private BulkUploadController bulkUploadController;

    private MockMvc mockMvc;

    private UUID sessionId;
    private UUID eventId;
    private UUID uploadedBy;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(bulkUploadController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sessionId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        uploadedBy = UUID.randomUUID();
    }

    private BulkUploadSessionDto createMockSession() {
        BulkUploadSessionDto dto = new BulkUploadSessionDto();
        dto.setId(sessionId);
        dto.setEventId(eventId);
        dto.setUploadedBy(uploadedBy);
        dto.setFileName("guests.xlsx");
        dto.setFileType("EXCEL");
        dto.setTotalGuests(100);
        dto.setSuccessCount(95);
        dto.setFailureCount(5);
        dto.setStatus(UploadStatus.COMPLETED.toString());
        dto.setUploadedAt(LocalDateTime.now());
        return dto;
    }

    @Test
    void testGetUploadSession_Success() throws Exception {
        // Arrange
        BulkUploadSessionDto sessionDto = createMockSession();
        when(bulkUploadService.getUploadSession(sessionId))
                .thenReturn(sessionDto);

        // Act & Assert
        mockMvc.perform(get("/api/v1/bulk-uploads/session/{sessionId}", sessionId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId.toString()))
                .andExpect(jsonPath("$.totalGuests").value(100));

        verify(bulkUploadService, times(1)).getUploadSession(sessionId);
    }

    @Test
    void testGetUploadSession_NotFound() throws Exception {
        // Arrange
        when(bulkUploadService.getUploadSession(sessionId))
                .thenThrow(new IllegalArgumentException("Session not found"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/bulk-uploads/session/{sessionId}", sessionId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Session not found"));
    }

    @Test
    void testGetUploadSessionsByEvent_Success() throws Exception {
        // Arrange
        BulkUploadSessionDto session1 = createMockSession();
        BulkUploadSessionDto session2 = createMockSession();
        session2.setId(UUID.randomUUID());

        when(bulkUploadService.getUploadSessionsByEvent(eventId))
                .thenReturn(Arrays.asList(session1, session2));

        // Act & Assert
        mockMvc.perform(get("/api/v1/bulk-uploads/event/{eventId}", eventId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        verify(bulkUploadService, times(1)).getUploadSessionsByEvent(eventId);
    }

    @Test
    void testGetUploadSessionsByEvent_Empty() throws Exception {
        // Arrange
        when(bulkUploadService.getUploadSessionsByEvent(eventId))
                .thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/v1/bulk-uploads/event/{eventId}", eventId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testGetUploadSessionsByUser_Success() throws Exception {
        // Arrange
        BulkUploadSessionDto session = createMockSession();
        when(bulkUploadService.getUploadSessionsByUser(uploadedBy))
                .thenReturn(Arrays.asList(session));

        // Act & Assert
        mockMvc.perform(get("/api/v1/bulk-uploads/user/{uploadedBy}", uploadedBy)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(bulkUploadService, times(1)).getUploadSessionsByUser(uploadedBy);
    }

    @Test
    void testGetUploadSessionsByUser_Empty() throws Exception {
        // Arrange
        when(bulkUploadService.getUploadSessionsByUser(uploadedBy))
                .thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/v1/bulk-uploads/user/{uploadedBy}", uploadedBy)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testGetAllUploadSessions_Success() throws Exception {
        // Arrange
        BulkUploadSessionDto session1 = createMockSession();
        BulkUploadSessionDto session2 = createMockSession();
        session2.setId(UUID.randomUUID());

        when(bulkUploadService.getAllUploadSessions())
                .thenReturn(Arrays.asList(session1, session2));

        // Act & Assert
        mockMvc.perform(get("/api/v1/bulk-uploads")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        verify(bulkUploadService, times(1)).getAllUploadSessions();
    }

    @Test
    void testGetAllUploadSessions_Empty() throws Exception {
        // Arrange
        when(bulkUploadService.getAllUploadSessions())
                .thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/v1/bulk-uploads")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testSessionDtoMapping() throws Exception {
        // Arrange
        BulkUploadSessionDto sessionDto = createMockSession();
        when(bulkUploadService.getUploadSession(sessionId))
                .thenReturn(sessionDto);

        // Act & Assert
        mockMvc.perform(get("/api/v1/bulk-uploads/session/{sessionId}", sessionId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId.toString()))
                .andExpect(jsonPath("$.eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.uploadedBy").value(uploadedBy.toString()))
                .andExpect(jsonPath("$.fileName").value("guests.xlsx"))
                .andExpect(jsonPath("$.fileType").value("EXCEL"))
                .andExpect(jsonPath("$.totalGuests").value(100))
                .andExpect(jsonPath("$.successCount").value(95))
                .andExpect(jsonPath("$.failureCount").value(5))
                .andExpect(jsonPath("$.status").value(UploadStatus.COMPLETED.toString()));
    }
}
