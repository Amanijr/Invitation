package com.InvitationSystem.InvitationSystem.Dto.templatesDto;

import lombok.Data;

import java.util.UUID;

@Data
public class TemplateRequestDto {
    private UUID eventId;
    private String templateName;
    private String content;
    private String previewImageUrl;
}
