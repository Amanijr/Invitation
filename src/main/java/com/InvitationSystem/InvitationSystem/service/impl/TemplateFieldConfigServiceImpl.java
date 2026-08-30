package com.InvitationSystem.InvitationSystem.service.impl;

import com.InvitationSystem.InvitationSystem.Dto.templatesDto.TemplateFieldConfigDto;
import com.InvitationSystem.InvitationSystem.entity.Template;
import com.InvitationSystem.InvitationSystem.entity.TemplateFieldConfig;
import com.InvitationSystem.InvitationSystem.repository.TemplateFieldConfigRepository;
import com.InvitationSystem.InvitationSystem.repository.TemplateRepository;
import com.InvitationSystem.InvitationSystem.service.TemplateFieldConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TemplateFieldConfigServiceImpl implements TemplateFieldConfigService {

    @Autowired
    private TemplateFieldConfigRepository fieldConfigRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TemplateFieldConfigDto> getFieldConfigsByTemplateId(UUID templateId) {
        ensureTemplateExists(templateId);
        return fieldConfigRepository.findByTemplateId(templateId).stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional
    public List<TemplateFieldConfigDto> saveFieldConfigs(UUID templateId, List<TemplateFieldConfigDto> configs) {
        ensureTemplateExists(templateId);

        // Delete existing configurations for clean replacement
        fieldConfigRepository.deleteByTemplateId(templateId);

        List<TemplateFieldConfigDto> savedDtos = List.of();
        if (configs != null && !configs.isEmpty()) {
            List<TemplateFieldConfig> entitiesToSave = new ArrayList<>();
            for (TemplateFieldConfigDto dto : configs) {
                validateDtoBounds(dto);
                TemplateFieldConfig entity = TemplateFieldConfig.builder()
                        .templateId(templateId)
                        .fieldType(dto.getFieldType())
                        .x(dto.getX())
                        .y(dto.getY())
                        .width(dto.getWidth())
                        .height(dto.getHeight())
                        .fontSize(dto.getFontSize() != null ? dto.getFontSize() : 24)
                        .fontColor(dto.getFontColor() != null ? dto.getFontColor() : "#FFFFFF")
                        .alignment(dto.getAlignment() != null ? dto.getAlignment().toUpperCase() : "CENTER")
                        .fontWeight(dto.getFontWeight() != null ? dto.getFontWeight().toUpperCase() : "BOLD")
                        .fontFamily(dto.getFontFamily() != null ? dto.getFontFamily() : "SansSerif")
                        .qrSize(dto.getQrSize() != null ? dto.getQrSize() : 180)
                        .sampleText(dto.getSampleText())
                        .build();

                entitiesToSave.add(entity);
            }
            savedDtos = fieldConfigRepository.saveAll(entitiesToSave).stream().map(this::mapToDto).toList();
        }

        templateRepository.findById(templateId).ifPresent(template -> {
            template.bumpVersion();
            templateRepository.save(template);
        });
        return savedDtos;
    }

    @Override
    @Transactional
    public TemplateFieldConfigDto updateFieldConfig(UUID templateId, UUID fieldId, TemplateFieldConfigDto dto) {
        ensureTemplateExists(templateId);
        TemplateFieldConfig config = fieldConfigRepository.findById(fieldId)
                .orElseThrow(() -> new IllegalArgumentException("Field config not found with ID: " + fieldId));

        if (!config.getTemplateId().equals(templateId)) {
            throw new IllegalArgumentException("Field config does not belong to template ID: " + templateId);
        }

        validateDtoBounds(dto);

        if (dto.getFieldType() != null) config.setFieldType(dto.getFieldType());
        if (dto.getX() != null) config.setX(dto.getX());
        if (dto.getY() != null) config.setY(dto.getY());
        if (dto.getWidth() != null) config.setWidth(dto.getWidth());
        if (dto.getHeight() != null) config.setHeight(dto.getHeight());
        if (dto.getFontSize() != null) config.setFontSize(dto.getFontSize());
        if (dto.getFontColor() != null) config.setFontColor(dto.getFontColor());
        if (dto.getAlignment() != null) config.setAlignment(dto.getAlignment().toUpperCase());
        if (dto.getFontWeight() != null) config.setFontWeight(dto.getFontWeight().toUpperCase());
        if (dto.getFontFamily() != null) config.setFontFamily(dto.getFontFamily());
        if (dto.getQrSize() != null) config.setQrSize(dto.getQrSize());
        if (dto.getSampleText() != null) config.setSampleText(dto.getSampleText());

        TemplateFieldConfig updated = fieldConfigRepository.save(config);
        return mapToDto(updated);
    }

    @Override
    @Transactional
    public void deleteFieldConfig(UUID templateId, UUID fieldId) {
        ensureTemplateExists(templateId);
        fieldConfigRepository.deleteByTemplateIdAndId(templateId, fieldId);
    }

    @Override
    @Transactional
    public void deleteAllFieldConfigsByTemplateId(UUID templateId) {
        ensureTemplateExists(templateId);
        fieldConfigRepository.deleteByTemplateId(templateId);
    }

    private void ensureTemplateExists(UUID templateId) {
        if (!templateRepository.existsById(templateId)) {
            throw new IllegalArgumentException("Template not found with ID: " + templateId);
        }
    }

    private void validateDtoBounds(TemplateFieldConfigDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Field configuration DTO cannot be null");
        }
        if (dto.getFieldType() == null) {
            throw new IllegalArgumentException("fieldType is required");
        }
        if (dto.getX() == null || dto.getX() < 0.0 || dto.getX() > 100.0) {
            throw new IllegalArgumentException("Field position X must be between 0.0 and 100.0 percent");
        }
        if (dto.getY() == null || dto.getY() < 0.0 || dto.getY() > 100.0) {
            throw new IllegalArgumentException("Field position Y must be between 0.0 and 100.0 percent");
        }
        if (dto.getWidth() == null || dto.getWidth() <= 0.0 || dto.getWidth() > 100.0) {
            throw new IllegalArgumentException("Field width must be between 0.0 and 100.0 percent");
        }
        if (dto.getHeight() == null || dto.getHeight() <= 0.0 || dto.getHeight() > 100.0) {
            throw new IllegalArgumentException("Field height must be between 0.0 and 100.0 percent");
        }
    }

    private TemplateFieldConfigDto mapToDto(TemplateFieldConfig entity) {
        return TemplateFieldConfigDto.builder()
                .id(entity.getId())
                .templateId(entity.getTemplateId())
                .fieldType(entity.getFieldType())
                .x(entity.getX())
                .y(entity.getY())
                .width(entity.getWidth())
                .height(entity.getHeight())
                .fontSize(entity.getFontSize())
                .fontColor(entity.getFontColor())
                .alignment(entity.getAlignment())
                .fontWeight(entity.getFontWeight())
                .fontFamily(entity.getFontFamily())
                .qrSize(entity.getQrSize())
                .sampleText(entity.getSampleText())
                .build();
    }
}
