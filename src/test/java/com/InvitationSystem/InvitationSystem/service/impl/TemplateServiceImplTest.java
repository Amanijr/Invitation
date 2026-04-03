package com.InvitationSystem.InvitationSystem.service.impl;

import com.InvitationSystem.InvitationSystem.Dto.templatesDto.TemplateRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.templatesDto.TemplateResponseDto;
import com.InvitationSystem.InvitationSystem.entity.EventType;
import com.InvitationSystem.InvitationSystem.entity.Template;
import com.InvitationSystem.InvitationSystem.repository.TemplateRepository;
import com.InvitationSystem.InvitationSystem.util.FileStorageUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateServiceImplTest {

    @Mock
    private TemplateRepository templateRepository;

    @Mock
    private FileStorageUtility fileStorageUtility;

    @InjectMocks
    private TemplateServiceImpl templateService;

    private UUID templateId;
    private UUID eventId;
    private TemplateRequestDto request;

    @BeforeEach
    void setUp() {
        templateId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        request = new TemplateRequestDto(
                eventId,
                EventType.WEDDING,
                "Classic Card",
                "<html>{{guestName}} {{qrCode}}</html>",
                null,
                null
        );
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

        assertEquals(templateId, response.getId());
        assertEquals(eventId, response.getEventId());
        assertEquals(EventType.WEDDING, response.getEventType());
    }

    @Test
    void createTemplate_duplicateName_throws() {
        when(templateRepository.existsByEventIdAndTemplateName(eventId, "Classic Card")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> templateService.createTemplate(request));
    }

    @Test
    void updateTemplate_notFound_throws() {
        when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> templateService.updateTemplate(templateId, request));
    }
}
