package com.InvitationSystem.InvitationSystem.Dto.templatesDto;


import lombok.Data;

import java.util.UUID;

@Data
public class TemplateResponseDto {

    private UUID id;
    private UUID eventId;
    private String templateName;
    private String content;
    private boolean active;

}
