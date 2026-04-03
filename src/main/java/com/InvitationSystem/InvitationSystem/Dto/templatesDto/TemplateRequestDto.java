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
@io.swagger.v3.oas.annotations.media.Schema(description = "Request payload for creating or updating invitation templates")
public class TemplateRequestDto {
    private UUID eventId;
    private EventType eventType;
    private String templateName;
    private String content;
    private MultipartFile file;
    private String previewImageUrl;
}
