package com.InvitationSystem.InvitationSystem.Dto.templatesDto;

import com.InvitationSystem.InvitationSystem.entity.FieldType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateFieldConfigDto {

    private UUID id;

    private UUID templateId;

    @NotNull(message = "fieldType is required")
    private FieldType fieldType;

    @NotNull(message = "x is required")
    @Min(value = 0, message = "x must be >= 0.0")
    @Max(value = 100, message = "x must be <= 100.0")
    private Double x;

    @NotNull(message = "y is required")
    @Min(value = 0, message = "y must be >= 0.0")
    @Max(value = 100, message = "y must be <= 100.0")
    private Double y;

    @NotNull(message = "width is required")
    @Min(value = 0, message = "width must be > 0.0")
    @Max(value = 100, message = "width must be <= 100.0")
    private Double width;

    @NotNull(message = "height is required")
    @Min(value = 0, message = "height must be > 0.0")
    @Max(value = 100, message = "height must be <= 100.0")
    private Double height;

    @Builder.Default
    private Integer fontSize = 24;

    @Builder.Default
    private String fontColor = "#FFFFFF";

    @Builder.Default
    private String alignment = "CENTER";

    @Builder.Default
    private String fontWeight = "BOLD";

    @Builder.Default
    private String fontFamily = "SansSerif";

    @Builder.Default
    private Integer qrSize = 180;

    private String sampleText;
}
