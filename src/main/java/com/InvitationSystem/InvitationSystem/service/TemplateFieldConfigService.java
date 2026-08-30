package com.InvitationSystem.InvitationSystem.service;

import com.InvitationSystem.InvitationSystem.Dto.templatesDto.TemplateFieldConfigDto;

import java.util.List;
import java.util.UUID;

public interface TemplateFieldConfigService {

    List<TemplateFieldConfigDto> getFieldConfigsByTemplateId(UUID templateId);

    List<TemplateFieldConfigDto> saveFieldConfigs(UUID templateId, List<TemplateFieldConfigDto> configs);

    TemplateFieldConfigDto updateFieldConfig(UUID templateId, UUID fieldId, TemplateFieldConfigDto config);

    void deleteFieldConfig(UUID templateId, UUID fieldId);

    void deleteAllFieldConfigsByTemplateId(UUID templateId);
}
