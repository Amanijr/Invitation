package com.InvitationSystem.InvitationSystem.service.impl;

import com.InvitationSystem.InvitationSystem.Dto.bulkUploadDto.BulkUploadRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.bulkUploadDto.BulkUploadResponseDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationResponseDto;
import com.InvitationSystem.InvitationSystem.entity.BulkUploadSession;
import com.InvitationSystem.InvitationSystem.entity.InvitationStatus;
import com.InvitationSystem.InvitationSystem.entity.UploadStatus;
import com.InvitationSystem.InvitationSystem.repository.BulkUploadSessionRepository;
import com.InvitationSystem.InvitationSystem.repository.InvitationRepository;
import com.InvitationSystem.InvitationSystem.service.InvitationService;
import com.InvitationSystem.InvitationSystem.util.ExcelParserService;
import com.InvitationSystem.InvitationSystem.util.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BulkUploadServiceImplTest {

    @Mock
    private BulkUploadSessionRepository bulkUploadSessionRepository;

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private InvitationService invitationService;

    @Mock
    private ExcelParserService excelParserService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private BulkUploadServiceImpl bulkUploadService;

    private BulkUploadRequestDto requestDto;
    private UUID eventId;
    private UUID uploadedBy;
    private UUID sessionId;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        uploadedBy = UUID.randomUUID();
        sessionId = UUID.randomUUID();

        requestDto = new BulkUploadRequestDto();
        requestDto.setEventId(eventId);
        requestDto.setFileName("guests.xlsx");
        requestDto.setFileType("EXCEL");
        requestDto.setFileContent(new byte[]{});
    }

    @Test
    void testProcessBulkUpload_Success() {
        // Arrange
        List<Map<String, String>> guestData = createGuestDataList(2);

        when(excelParserService.parseExcelBytes(any()))
                .thenReturn(guestData);
        when(bulkUploadSessionRepository.save(any(BulkUploadSession.class)))
                .thenAnswer(invocation -> {
                    BulkUploadSession session = invocation.getArgument(0);
                    session.setId(sessionId);
                    return session;
                });
        when(invitationService.createInvitation(any()))
                .thenReturn(mockInvitationResponse());
        when(invitationRepository.findByUniqueToken(anyString()))
                .thenReturn(Optional.of(mockInvitation()));

        doNothing().when(emailService).sendBulkUploadConfirmation(anyString(), anyString(), anyInt(), anyInt());

        // Act
        BulkUploadResponseDto response = bulkUploadService.processBulkUpload(requestDto, uploadedBy);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getTotalGuests());
        assertEquals(UploadStatus.COMPLETED.toString(), response.getStatus());
        verify(bulkUploadSessionRepository, atLeast(1)).save(any());
        verify(emailService, times(1)).sendBulkUploadConfirmation(anyString(), anyString(), anyInt(), anyInt());
    }

    @Test
    void testProcessBulkUpload_WithFailures() {
        // Arrange
        List<Map<String, String>> guestData = createGuestDataList(3);

        when(excelParserService.parseExcelBytes(any()))
                .thenReturn(guestData);
        when(bulkUploadSessionRepository.save(any(BulkUploadSession.class)))
                .thenAnswer(invocation -> {
                    BulkUploadSession session = invocation.getArgument(0);
                    session.setId(sessionId);
                    return session;
                });

        // First invitation succeeds, second and third fail
        when(invitationService.createInvitation(any()))
                .thenReturn(mockInvitationResponse())
                .thenThrow(new RuntimeException("Duplicate"))
                .thenThrow(new RuntimeException("Invalid data"));

        doNothing().when(emailService).sendBulkUploadConfirmation(anyString(), anyString(), anyInt(), anyInt());

        // Act
        BulkUploadResponseDto response = bulkUploadService.processBulkUpload(requestDto, uploadedBy);

        // Assert
        assertNotNull(response);
        assertEquals(3, response.getTotalGuests());
        assertEquals(1, response.getSuccessCount());
        assertEquals(2, response.getFailureCount());
        assertNotNull(response.getErrorMessage());
    }

    @Test
    void testProcessBulkUpload_EmptyFile_ThrowsException() {
        // Arrange
        when(excelParserService.parseExcelBytes(any()))
                .thenReturn(new ArrayList<>());

        when(bulkUploadSessionRepository.save(any(BulkUploadSession.class)))
                .thenAnswer(invocation -> {
                    BulkUploadSession session = invocation.getArgument(0);
                    session.setId(sessionId);
                    return session;
                });

        // Act & Assert
        assertThrows(RuntimeException.class, () -> bulkUploadService.processBulkUpload(requestDto, uploadedBy));
    }

    @Test
    void testGetUploadSession_Success() {
        // Arrange
        BulkUploadSession session = BulkUploadSession.builder()
                .id(sessionId)
                .eventId(eventId)
                .uploadedBy(uploadedBy)
                .fileName("guests.xlsx")
                .fileType("EXCEL")
                .totalGuests(10)
                .successCount(9)
                .failureCount(1)
                .status(UploadStatus.COMPLETED)
                .uploadedAt(LocalDateTime.now())
                .build();

        when(bulkUploadSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        // Act
        var response = bulkUploadService.getUploadSession(sessionId);

        // Assert
        assertNotNull(response);
        assertEquals(sessionId, response.getId());
        assertEquals(10, response.getTotalGuests());
    }

    @Test
    void testGetUploadSession_NotFound_ThrowsException() {
        // Arrange
        when(bulkUploadSessionRepository.findById(sessionId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> bulkUploadService.getUploadSession(sessionId));
    }

    @Test
    void testGetUploadSessionsByEvent_Success() {
        // Arrange
        BulkUploadSession session1 = createBulkUploadSession();
        BulkUploadSession session2 = createBulkUploadSession();

        when(bulkUploadSessionRepository.findByEventIdOrderByUploadedAtDesc(eventId))
                .thenReturn(Arrays.asList(session1, session2));

        // Act
        var response = bulkUploadService.getUploadSessionsByEvent(eventId);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.size());
    }

    @Test
    void testGetUploadSessionsByUser_Success() {
        // Arrange
        BulkUploadSession session = createBulkUploadSession();

        when(bulkUploadSessionRepository.findByUploadedByOrderByUploadedAtDesc(uploadedBy))
                .thenReturn(Collections.singletonList(session));

        // Act
        var response = bulkUploadService.getUploadSessionsByUser(uploadedBy);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.size());
    }

    // Helper methods
    private List<Map<String, String>> createGuestDataList(int count) {
        List<Map<String, String>> guestData = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Map<String, String> guest = new HashMap<>();
            guest.put("email", "guest" + i + "@example.com");
            guest.put("phone", "+123456789" + i);
            guest.put("name", "Guest " + i);
            guestData.add(guest);
        }
        return guestData;
    }

    private InvitationResponseDto mockInvitationResponse() {
        InvitationResponseDto dto = new InvitationResponseDto();
        dto.setId(UUID.randomUUID());
        dto.setUniqueToken("token-" + UUID.randomUUID());
        dto.setStatus(InvitationStatus.GENERATED);
        return dto;
    }

    private com.InvitationSystem.InvitationSystem.entity.Invitation mockInvitation() {
        return com.InvitationSystem.InvitationSystem.entity.Invitation.builder()
                .id(UUID.randomUUID())
                .uniqueToken("token-123")
                .eventId(eventId)
                .build();
    }

    private BulkUploadSession createBulkUploadSession() {
        return BulkUploadSession.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .uploadedBy(uploadedBy)
                .fileName("file.xlsx")
                .fileType("EXCEL")
                .totalGuests(10)
                .successCount(9)
                .failureCount(1)
                .status(UploadStatus.COMPLETED)
                .uploadedAt(LocalDateTime.now())
                .build();
    }
}
