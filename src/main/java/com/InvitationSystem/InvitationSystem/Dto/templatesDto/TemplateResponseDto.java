package com.InvitationSystem.InvitationSystem.Dto.templatesDto;

import com.InvitationSystem.InvitationSystem.entity.EventType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import lombok.Builder;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@io.swagger.v3.oas.annotations.media.Schema(description = "Response payload for invitation template details")
public class TemplateResponseDto {

    private UUID id;
    private UUID eventId;
    private EventType eventType;
    private String templateName;
    private String originalFileName;
    private String storagePath;
    private String mimeType;
    private Long fileSize;
    private String content;
    private String previewImageUrl;
    private String fileUrl;
    private Integer width;
    private Integer height;
    private boolean active;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
