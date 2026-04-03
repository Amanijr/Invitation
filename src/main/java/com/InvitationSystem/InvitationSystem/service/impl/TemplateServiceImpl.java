package com.InvitationSystem.InvitationSystem.service.impl;

import com.InvitationSystem.InvitationSystem.Dto.templatesDto.TemplateRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.templatesDto.TemplateResponseDto;
import com.InvitationSystem.InvitationSystem.entity.EventType;
import com.InvitationSystem.InvitationSystem.entity.Template;
import com.InvitationSystem.InvitationSystem.repository.TemplateRepository;
import com.InvitationSystem.InvitationSystem.service.TemplateService;
import com.InvitationSystem.InvitationSystem.util.FileStorageUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
public class TemplateServiceImpl implements TemplateService {

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private FileStorageUtility fileStorageUtility;



    @Override
    public TemplateResponseDto createTemplate(TemplateRequestDto request) {
        // Duplicate check using existsByEventIdAndTemplateName
        if (templateRepository.existsByEventIdAndTemplateName(request.getEventId(), request.getTemplateName())) {
            throw new IllegalArgumentException(
                    "A template with the name '" + request.getTemplateName() + "' already exists for this event."
            );
        }

        MultipartFile file = request.getFile();
        String filePath = null;
        if (file != null && !file.isEmpty()) {
            filePath = fileStorageUtility.saveFile(file, request.getEventType().name());
        }

        Template template = Template.builder()
            .eventId(request.getEventId())
                .eventType(request.getEventType())
                .templateName(request.getTemplateName())
                .content(request.getContent())
                .previewImageUrl(filePath)
                .active(true)
                .build();

        Template savedTemplate = templateRepository.save(template);
        return mapToResponseDto(savedTemplate);
    }

    @Override
    public TemplateResponseDto getTemplateById(UUID templateId) {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found with ID: " + templateId));
        return mapToResponseDto(template);
    }

    @Override
    public List<TemplateResponseDto> getTemplatesByEventType(EventType eventType) {
        return templateRepository.findByEventType(eventType).stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public List<TemplateResponseDto> getActiveTemplatesByEventType(EventType eventType) {
        return templateRepository.findByEventTypeAndActive(eventType, true).stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public List<TemplateResponseDto> getAllTemplates() {
        return templateRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public TemplateResponseDto updateTemplate(UUID templateId, TemplateRequestDto request) {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found with ID: " + templateId));

        template.setEventId(request.getEventId());
        template.setEventType(request.getEventType());
        template.setTemplateName(request.getTemplateName());
        template.setContent(request.getContent());

        MultipartFile file = request.getFile();
        if (file != null && !file.isEmpty()) {
            String filePath = fileStorageUtility.saveFile(file, request.getEventType().name());
            template.setPreviewImageUrl(filePath);
        }

        Template updatedTemplate = templateRepository.save(template);
        return mapToResponseDto(updatedTemplate);
    }

    @Override
    public TemplateResponseDto deactivateTemplate(UUID templateId) {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found with ID: " + templateId));
        template.setActive(false);
        return mapToResponseDto(templateRepository.save(template));
    }

    @Override
    public void deleteTemplate(UUID templateId) {
        templateRepository.deleteById(templateId);
    }



    @Override
    public List<TemplateResponseDto> getAllActiveTemplates() {
        return templateRepository.findByActive(true).stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public List<TemplateResponseDto> searchTemplatesByName(String name) {
        return templateRepository.findByTemplateNameContainingIgnoreCase(name).stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
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
    public List<TemplateResponseDto> getTemplatesByEventId(UUID eventId) {
        return templateRepository.findByEventId(eventId).stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public List<TemplateResponseDto> getActiveTemplatesByEventId(UUID eventId) {
        return templateRepository.findByEventIdAndActive(eventId, true).stream()
                .map(this::mapToResponseDto)
                .toList();
    }


    private TemplateResponseDto mapToResponseDto(Template template) {
        TemplateResponseDto dto = new TemplateResponseDto();
        dto.setId(template.getId());
        dto.setEventId(template.getEventId());
        dto.setEventType(template.getEventType());
        dto.setTemplateName(template.getTemplateName());
        dto.setContent(template.getContent());
        dto.setActive(template.isActive());
        return dto;
    }
}