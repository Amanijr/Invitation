package com.InvitationSystem.InvitationSystem.Dto.templatesDto;

import com.InvitationSystem.InvitationSystem.entity.EventType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@io.swagger.v3.oas.annotations.media.Schema(description = "Response payload for invitation template details")
public class TemplateResponseDto {

    private UUID id;
    private UUID eventId;
    private EventType eventType;
    private String templateName;
    private MultipartFile file;
    private String content;
    private boolean active;
}
