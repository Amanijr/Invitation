package com.InvitationSystem.InvitationSystem.Dto.templatesDto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplatePreviewResponseDto {

    private UUID templateId;
    private String base64Image;
    private String mimeType;
    private Integer width;
    private Integer height;
}
