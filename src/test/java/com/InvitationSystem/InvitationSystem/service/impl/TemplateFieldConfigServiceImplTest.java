package com.InvitationSystem.InvitationSystem.service.impl;

import com.InvitationSystem.InvitationSystem.Dto.templatesDto.TemplateFieldConfigDto;
import com.InvitationSystem.InvitationSystem.entity.FieldType;
import com.InvitationSystem.InvitationSystem.entity.TemplateFieldConfig;
import com.InvitationSystem.InvitationSystem.repository.TemplateFieldConfigRepository;
import com.InvitationSystem.InvitationSystem.repository.TemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemplateFieldConfigServiceImplTest {

    @Mock
    private TemplateFieldConfigRepository fieldConfigRepository;

    @Mock
    private TemplateRepository templateRepository;

    @InjectMocks
    private TemplateFieldConfigServiceImpl fieldConfigService;

    private UUID templateId;
    private UUID fieldId;
    private TemplateFieldConfigDto dto;
    private TemplateFieldConfig entity;

    @BeforeEach
    void setUp() {
        templateId = UUID.randomUUID();
        fieldId = UUID.randomUUID();

        dto = TemplateFieldConfigDto.builder()
                .id(fieldId)
                .templateId(templateId)
                .fieldType(FieldType.GUEST_NAME)
                .x(20.0)
                .y(40.0)
                .width(60.0)
                .height(10.0)
                .fontSize(32)
                .fontColor("#FFFFFF")
                .alignment("CENTER")
                .fontWeight("BOLD")
                .build();

        entity = TemplateFieldConfig.builder()
                .id(fieldId)
                .templateId(templateId)
                .fieldType(FieldType.GUEST_NAME)
                .x(20.0)
                .y(40.0)
                .width(60.0)
                .height(10.0)
                .fontSize(32)
                .fontColor("#FFFFFF")
                .alignment("CENTER")
                .fontWeight("BOLD")
                .build();
    }

    @Test
    void getFieldConfigsByTemplateId_success() {
        when(templateRepository.existsById(templateId)).thenReturn(true);
        when(fieldConfigRepository.findByTemplateId(templateId)).thenReturn(List.of(entity));

        List<TemplateFieldConfigDto> results = fieldConfigService.getFieldConfigsByTemplateId(templateId);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(FieldType.GUEST_NAME, results.get(0).getFieldType());
    }

    @Test
    void saveFieldConfigs_replacesExistingConfigs() {
        when(templateRepository.existsById(templateId)).thenReturn(true);
        doNothing().when(fieldConfigRepository).deleteByTemplateId(templateId);
        when(fieldConfigRepository.saveAll(any())).thenReturn(List.of(entity));

        List<TemplateFieldConfigDto> saved = fieldConfigService.saveFieldConfigs(templateId, List.of(dto));

        assertNotNull(saved);
        assertEquals(1, saved.size());
        verify(fieldConfigRepository, times(1)).deleteByTemplateId(templateId);
        verify(fieldConfigRepository, times(1)).saveAll(any());
    }

    @Test
    void saveFieldConfigs_invalidBounds_throwsException() {
        when(templateRepository.existsById(templateId)).thenReturn(true);
        dto.setX(150.0); // Out of bounds > 100%

        assertThrows(IllegalArgumentException.class, () -> fieldConfigService.saveFieldConfigs(templateId, List.of(dto)));
    }

    @Test
    void updateFieldConfig_updatesProperties() {
        when(templateRepository.existsById(templateId)).thenReturn(true);
        when(fieldConfigRepository.findById(fieldId)).thenReturn(Optional.of(entity));
        when(fieldConfigRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        dto.setFontSize(48);
        dto.setFontColor("#FF0000");

        TemplateFieldConfigDto updated = fieldConfigService.updateFieldConfig(templateId, fieldId, dto);

        assertNotNull(updated);
        assertEquals(48, updated.getFontSize());
        assertEquals("#FF0000", updated.getFontColor());
    }

    @Test
    void deleteFieldConfig_callsRepository() {
        when(templateRepository.existsById(templateId)).thenReturn(true);
        doNothing().when(fieldConfigRepository).deleteByTemplateIdAndId(templateId, fieldId);

        fieldConfigService.deleteFieldConfig(templateId, fieldId);

        verify(fieldConfigRepository, times(1)).deleteByTemplateIdAndId(templateId, fieldId);
    }
}
