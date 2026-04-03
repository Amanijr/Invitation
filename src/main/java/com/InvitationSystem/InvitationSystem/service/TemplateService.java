package com.InvitationSystem.InvitationSystem.service;

import com.InvitationSystem.InvitationSystem.Dto.templatesDto.TemplateRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.templatesDto.TemplateResponseDto;
import com.InvitationSystem.InvitationSystem.entity.EventType;

import java.util.List;
import java.util.UUID;

public interface TemplateService {

    // --- Existing ---
    TemplateResponseDto createTemplate(TemplateRequestDto request);
    TemplateResponseDto getTemplateById(UUID templateId);
    List<TemplateResponseDto> getTemplatesByEventType(EventType eventType);
    List<TemplateResponseDto> getActiveTemplatesByEventType(EventType eventType);
    List<TemplateResponseDto> getAllTemplates();
    TemplateResponseDto updateTemplate(UUID templateId, TemplateRequestDto request);
    TemplateResponseDto deactivateTemplate(UUID templateId);
    void deleteTemplate(UUID templateId);

    // --- Search & Filtering ---
    List<TemplateResponseDto> getAllActiveTemplates();
    List<TemplateResponseDto> searchTemplatesByName(String name);
    List<TemplateResponseDto> searchTemplatesByEventTypeAndName(EventType eventType, String name);

    // --- Count Queries ---
    long countTemplatesByEventType(EventType eventType);
    long countActiveTemplatesByEventType(EventType eventType);

    // --- Event-Specific ---
    List<TemplateResponseDto> getTemplatesByEventId(UUID eventId);
    List<TemplateResponseDto> getActiveTemplatesByEventId(UUID eventId);
}