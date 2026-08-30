package com.InvitationSystem.InvitationSystem.service.impl;

import com.InvitationSystem.InvitationSystem.Dto.templatesDto.TemplateRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.templatesDto.TemplateResponseDto;
import com.InvitationSystem.InvitationSystem.entity.EventType;
import com.InvitationSystem.InvitationSystem.entity.Template;
import com.InvitationSystem.InvitationSystem.repository.TemplateFieldConfigRepository;
import com.InvitationSystem.InvitationSystem.repository.TemplateRepository;
import com.InvitationSystem.InvitationSystem.service.storage.FileMetadata;
import com.InvitationSystem.InvitationSystem.service.storage.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemplateServiceImplTest {

    @Mock
    private TemplateRepository templateRepository;

    @Mock
    private TemplateFieldConfigRepository fieldConfigRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private TemplateServiceImpl templateService;

    private UUID templateId;
    private UUID eventId;
    private TemplateRequestDto request;
    private Template template;

    @BeforeEach
    void setUp() {
        templateId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        request = TemplateRequestDto.builder()
                .eventId(eventId)
                .eventType(EventType.WEDDING)
                .templateName("Classic Card")
                .content("<html>{{guestName}} {{qrCode}}</html>")
                .width(1920)
                .height(1080)
                .build();

        template = Template.builder()
                .id(templateId)
                .eventId(eventId)
                .eventType(EventType.WEDDING)
                .templateName("Classic Card")
                .storagePath("WEDDING/unique.png")
                .originalFileName("card.png")
                .mimeType("image/png")
                .fileSize(1024L)
                .width(1920)
                .height(1080)
                .active(true)
                .build();
    }

    @Test
    void createTemplate_setsEventIdAndType() {
        when(templateRepository.existsByEventIdAndTemplateName(eventId, "Classic Card")).thenReturn(false);
        when(templateRepository.save(any(Template.class))).thenAnswer(invocation -> {
            Template t = invocation.getArgument(0);
            t.setId(templateId);
            return t;
        });

        TemplateResponseDto response = templateService.createTemplate(request);

        assertNotNull(response);
        assertEquals(templateId, response.getId());
        assertEquals(eventId, response.getEventId());
        assertEquals(EventType.WEDDING, response.getEventType());
    }

    @Test
    void createTemplate_withMultipartFile_storesFileAndSetsMetadata() {
        MockMultipartFile file = new MockMultipartFile("file", "card.png", "image/png", "bytes".getBytes());
        request.setFile(file);

        FileMetadata metadata = FileMetadata.builder()
                .storagePath("WEDDING/file.png")
                .originalFileName("card.png")
                .mimeType("image/png")
                .fileSize(5L)
                .width(1920)
                .height(1080)
                .build();

        when(templateRepository.existsByEventIdAndTemplateName(eventId, "Classic Card")).thenReturn(false);
        when(fileStorageService.storeFile(any(), anyString())).thenReturn(metadata);
        when(templateRepository.save(any(Template.class))).thenAnswer(i -> {
            Template t = i.getArgument(0);
            t.setId(templateId);
            return t;
        });

        TemplateResponseDto response = templateService.createTemplate(request);

        assertNotNull(response);
        assertEquals("WEDDING/file.png", response.getStoragePath());
        assertEquals("card.png", response.getOriginalFileName());
        assertEquals("/api/v1/templates/" + templateId + "/file", response.getPreviewImageUrl());
    }

    @Test
    void createTemplate_duplicateName_throws() {
        when(templateRepository.existsByEventIdAndTemplateName(eventId, "Classic Card")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> templateService.createTemplate(request));
    }

    @Test
    void activateAndDeactivateTemplate_togglesState() {
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(templateRepository.save(any(Template.class))).thenAnswer(i -> i.getArgument(0));

        TemplateResponseDto deactivated = templateService.deactivateTemplate(templateId);
        assertFalse(deactivated.isActive());

        TemplateResponseDto activated = templateService.activateTemplate(templateId);
        assertTrue(activated.isActive());
    }

    @Test
    void deleteTemplate_deletesFileAndDatabaseRecord() {
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        doNothing().when(fileStorageService).deleteFile("WEDDING/unique.png");
        doNothing().when(templateRepository).deleteById(templateId);

        templateService.deleteTemplate(templateId);

        verify(fieldConfigRepository, times(1)).deleteByTemplateId(templateId);
        verify(fileStorageService, times(1)).deleteFile("WEDDING/unique.png");
        verify(templateRepository, times(1)).deleteById(templateId);
    }

    @Test
    void getTemplateById_withoutStoragePath_omitsFileUrl() {
        template.setStoragePath(null);
        template.setPreviewImageUrl("https://example.com/cover.png");
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));

        TemplateResponseDto response = templateService.getTemplateById(templateId);

        assertNull(response.getFileUrl());
        assertEquals("https://example.com/cover.png", response.getPreviewImageUrl());
    }

    @Test
    void loadTemplateFile_missingStorage_throws() {
        template.setStoragePath(null);
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));

        assertThrows(IllegalArgumentException.class, () -> templateService.loadTemplateFile(templateId));
    }
}
