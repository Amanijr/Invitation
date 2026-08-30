package com.InvitationSystem.InvitationSystem.service.impl;

import com.InvitationSystem.InvitationSystem.Dto.templatesDto.TemplateRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.templatesDto.TemplateResponseDto;
import com.InvitationSystem.InvitationSystem.entity.EventType;
import com.InvitationSystem.InvitationSystem.entity.Template;
import com.InvitationSystem.InvitationSystem.repository.TemplateFieldConfigRepository;
import com.InvitationSystem.InvitationSystem.repository.TemplateRepository;
import com.InvitationSystem.InvitationSystem.service.TemplateService;
import com.InvitationSystem.InvitationSystem.service.storage.FileMetadata;
import com.InvitationSystem.InvitationSystem.service.storage.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
public class TemplateServiceImpl implements TemplateService {

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private TemplateFieldConfigRepository fieldConfigRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Override
    @Transactional
    public TemplateResponseDto createTemplate(TemplateRequestDto request) {
        UUID eventId = request.getEventId() != null
                ? request.getEventId()
                : UUID.fromString("00000000-0000-0000-0000-000000000000");
        if (templateRepository.existsByEventIdAndTemplateName(eventId, request.getTemplateName())) {
            throw new IllegalArgumentException(
                    "A template with the name '" + request.getTemplateName() + "' already exists for this event."
            );
        }

        MultipartFile file = request.getFile();
        FileMetadata metadata = null;
        if (file != null && !file.isEmpty()) {
            String folder = request.getEventType() != null ? request.getEventType().name() : "GENERAL";
            metadata = fileStorageService.storeFile(file, folder);
        }

        Template template = Template.builder()
                .eventId(eventId)
                .eventType(request.getEventType())
                .templateName(request.getTemplateName())
                .content(request.getContent())
                .originalFileName(metadata != null ? metadata.getOriginalFileName() : null)
                .storagePath(metadata != null ? metadata.getStoragePath() : null)
                .mimeType(metadata != null ? metadata.getMimeType() : null)
                .fileSize(metadata != null ? metadata.getFileSize() : null)
                .width(metadata != null ? metadata.getWidth() : (request.getWidth() != null ? request.getWidth() : 1920))
                .height(metadata != null ? metadata.getHeight() : (request.getHeight() != null ? request.getHeight() : 1080))
                .previewImageUrl(request.getPreviewImageUrl())
                .active(true)
                .version(1)
                .build();

        Template savedTemplate = templateRepository.save(template);
        if (metadata != null && savedTemplate.getId() != null) {
            savedTemplate.setPreviewImageUrl("/api/v1/templates/" + savedTemplate.getId() + "/file");
            savedTemplate = templateRepository.save(savedTemplate);
        }
        return mapToResponseDto(savedTemplate);
    }

    @Override
    @Transactional(readOnly = true)
    public TemplateResponseDto getTemplateById(UUID templateId) {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found with ID: " + templateId));
        return mapToResponseDto(template);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateResponseDto> getTemplatesByEventType(EventType eventType) {
        return templateRepository.findByEventType(eventType).stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateResponseDto> getActiveTemplatesByEventType(EventType eventType) {
        return templateRepository.findByEventTypeAndActive(eventType, true).stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateResponseDto> getAllTemplates() {
        return templateRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public TemplateResponseDto updateTemplate(UUID templateId, TemplateRequestDto request) {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found with ID: " + templateId));

        if (request.getEventId() != null) template.setEventId(request.getEventId());
        if (request.getEventType() != null) template.setEventType(request.getEventType());
        if (request.getTemplateName() != null) template.setTemplateName(request.getTemplateName());
        if (request.getContent() != null) template.setContent(request.getContent());
        if (request.getWidth() != null) template.setWidth(request.getWidth());
        if (request.getHeight() != null) template.setHeight(request.getHeight());

        MultipartFile file = request.getFile();
        if (file != null && !file.isEmpty()) {
            // Delete old file if present
            if (template.getStoragePath() != null) {
                fileStorageService.deleteFile(template.getStoragePath());
            }
            String folder = template.getEventType() != null ? template.getEventType().name() : "GENERAL";
            FileMetadata metadata = fileStorageService.storeFile(file, folder);
            template.setOriginalFileName(metadata.getOriginalFileName());
            template.setStoragePath(metadata.getStoragePath());
            template.setMimeType(metadata.getMimeType());
            template.setFileSize(metadata.getFileSize());
            template.setWidth(metadata.getWidth());
            template.setHeight(metadata.getHeight());
            template.bumpVersion();
        } else if (request.getContent() != null) {
            template.bumpVersion();
        }

        Template updatedTemplate = templateRepository.save(template);
        return mapToResponseDto(updatedTemplate);
    }

    @Override
    @Transactional
    public TemplateResponseDto deactivateTemplate(UUID templateId) {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found with ID: " + templateId));
        template.setActive(false);
        return mapToResponseDto(templateRepository.save(template));
    }

    @Override
    @Transactional
    public TemplateResponseDto activateTemplate(UUID templateId) {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found with ID: " + templateId));
        template.setActive(true);
        return mapToResponseDto(templateRepository.save(template));
    }

    @Override
    @Transactional
    public void deleteTemplate(UUID templateId) {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found with ID: " + templateId));

        fieldConfigRepository.deleteByTemplateId(templateId);
        if (template.getStoragePath() != null) {
            fileStorageService.deleteFile(template.getStoragePath());
        }
        templateRepository.deleteById(templateId);
    }

    @Override
    @Transactional
    public int deleteAllTemplates() {
        List<Template> templates = templateRepository.findAll();
        for (Template template : templates) {
            fieldConfigRepository.deleteByTemplateId(template.getId());
            if (template.getStoragePath() != null) {
                fileStorageService.deleteFile(template.getStoragePath());
            }
        }
        templateRepository.deleteAll(templates);
        return templates.size();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] loadTemplateFile(UUID templateId) {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found with ID: " + templateId));

        if (template.getStoragePath() == null || !fileStorageService.exists(template.getStoragePath())) {
            throw new IllegalArgumentException("Template file not found for ID: " + templateId);
        }

        return fileStorageService.loadFile(template.getStoragePath());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateResponseDto> getAllActiveTemplates() {
        return templateRepository.findByActive(true).stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateResponseDto> searchTemplatesByName(String name) {
        return templateRepository.findByTemplateNameContainingIgnoreCase(name).stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateResponseDto> searchTemplatesByEventTypeAndName(EventType eventType, String name) {
        return templateRepository.findByEventTypeAndTemplateNameContainingIgnoreCase(eventType, name).stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public long countTemplatesByEventType(EventType eventType) {
        return templateRepository.countByEventType(eventType);
    }

    @Override
    public long countActiveTemplatesByEventType(EventType eventType) {
        return templateRepository.countByEventTypeAndActive(eventType, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateResponseDto> getTemplatesByEventId(UUID eventId) {
        return templateRepository.findByEventId(eventId).stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateResponseDto> getActiveTemplatesByEventId(UUID eventId) {
        return templateRepository.findByEventIdAndActive(eventId, true).stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    private String resolvePreviewImageUrl(Template template) {
        if (template.getId() != null && template.getStoragePath() != null) {
            return "/api/v1/templates/" + template.getId() + "/file";
        }
        return template.getPreviewImageUrl();
    }

    private TemplateResponseDto mapToResponseDto(Template template) {
        return TemplateResponseDto.builder()
                .id(template.getId())
                .eventId(template.getEventId())
                .eventType(template.getEventType())
                .templateName(template.getTemplateName())
                .originalFileName(template.getOriginalFileName())
                .storagePath(template.getStoragePath())
                .mimeType(template.getMimeType())
                .fileSize(template.getFileSize())
                .content(template.getContent())
                .previewImageUrl(resolvePreviewImageUrl(template))
                .fileUrl(template.getStoragePath() != null
                        ? "/api/v1/templates/" + template.getId() + "/file"
                        : null)
                .width(template.getWidth())
                .height(template.getHeight())
                .active(template.isActive())
                .version(template.resolvedVersion())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}