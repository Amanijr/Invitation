package com.InvitationSystem.InvitationSystem.service.impl;

import com.InvitationSystem.InvitationSystem.Dto.guestDto.GuestImportConfirmRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.guestDto.GuestImportPreviewDto;
import com.InvitationSystem.InvitationSystem.Dto.guestDto.GuestImportRowDto;
import com.InvitationSystem.InvitationSystem.Dto.guestDto.GuestImportSummaryDto;
import com.InvitationSystem.InvitationSystem.entity.Guest;
import com.InvitationSystem.InvitationSystem.repository.EventRepository;
import com.InvitationSystem.InvitationSystem.repository.GuestRepository;
import com.InvitationSystem.InvitationSystem.util.ExcelParserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuestImportServiceImplTest {

    @Mock
    private ExcelParserService excelParserService;

    @Mock
    private GuestRepository guestRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private GuestImportServiceImpl guestImportService;

    private UUID eventId;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
    }

    @Test
    void previewImport_validRows_allValid() {
        when(eventRepository.existsById(eventId)).thenReturn(true);

        Map<String, String> row1 = new HashMap<>();
        row1.put("fullName", "John Michael");
        row1.put("phone", "+1234567890");
        row1.put("email", "john@example.com");

        when(excelParserService.parseFile(any())).thenReturn(List.of(row1));
        when(guestRepository.existsByEventIdAndEmail(eq(eventId), anyString())).thenReturn(false);
        when(guestRepository.existsByEventIdAndPhone(eq(eventId), anyString())).thenReturn(false);
        when(guestRepository.existsByEventIdAndFullName(eq(eventId), anyString())).thenReturn(false);

        MockMultipartFile file = new MockMultipartFile("file", "guests.csv", "text/csv", "dummy".getBytes());
        GuestImportPreviewDto preview = guestImportService.previewImport(file, eventId, "BOTH");

        assertNotNull(preview);
        assertEquals(1, preview.getTotalRows());
        assertEquals(1, preview.getValidCount());
        assertEquals(0, preview.getInvalidCount());
        assertTrue(preview.getRows().get(0).isValid());
    }

    @Test
    void previewImport_invalidPhoneAndEmail_flagsErrors() {
        when(eventRepository.existsById(eventId)).thenReturn(true);

        Map<String, String> row1 = new HashMap<>();
        row1.put("fullName", "Invalid User");
        row1.put("phone", "invalid-phone");
        row1.put("email", "invalid-email");

        when(excelParserService.parseFile(any())).thenReturn(List.of(row1));

        MockMultipartFile file = new MockMultipartFile("file", "guests.csv", "text/csv", "dummy".getBytes());
        GuestImportPreviewDto preview = guestImportService.previewImport(file, eventId, "BOTH");

        assertNotNull(preview);
        assertEquals(1, preview.getTotalRows());
        assertEquals(0, preview.getValidCount());
        assertEquals(1, preview.getInvalidCount());
        assertFalse(preview.getRows().get(0).isValid());
        assertTrue(preview.getRows().get(0).getErrors().size() >= 2);
    }

    @Test
    void previewImport_duplicateDetection_flagsDuplicates() {
        when(eventRepository.existsById(eventId)).thenReturn(true);

        Map<String, String> row1 = new HashMap<>();
        row1.put("fullName", "Duplicate User");
        row1.put("phone", "+1234567890");
        row1.put("email", "dup@example.com");

        when(excelParserService.parseFile(any())).thenReturn(List.of(row1));
        when(guestRepository.existsByEventIdAndEmail(eventId, "dup@example.com")).thenReturn(true);

        MockMultipartFile file = new MockMultipartFile("file", "guests.csv", "text/csv", "dummy".getBytes());
        GuestImportPreviewDto preview = guestImportService.previewImport(file, eventId, "EMAIL");

        assertNotNull(preview);
        assertEquals(1, preview.getDuplicateCount());
        assertTrue(preview.getRows().get(0).isDuplicate());
    }

    @Test
    void confirmImport_savesValidRows() {
        when(eventRepository.existsById(eventId)).thenReturn(true);

        GuestImportRowDto row = GuestImportRowDto.builder()
                .fullName("Sarah Vance")
                .phone("+9876543210")
                .email("sarah@example.com")
                .valid(true)
                .build();

        Guest savedGuest = Guest.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .fullName("Sarah Vance")
                .phone("+9876543210")
                .email("sarah@example.com")
                .build();

        when(guestRepository.saveAll(any())).thenReturn(List.of(savedGuest));

        GuestImportConfirmRequestDto req = GuestImportConfirmRequestDto.builder()
                .eventId(eventId)
                .deliveryChannel("BOTH")
                .rowsToImport(List.of(row))
                .build();

        GuestImportSummaryDto summary = guestImportService.confirmImport(req);

        assertNotNull(summary);
        assertEquals(1, summary.getImportedCount());
        assertEquals(0, summary.getSkippedCount());
        verify(guestRepository, times(1)).saveAll(any());
    }
}
