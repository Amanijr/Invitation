package com.InvitationSystem.InvitationSystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "template_field_configs", indexes = {
        @Index(name = "idx_field_config_template_id", columnList = "templateId"),
        @Index(name = "idx_field_config_field_type", columnList = "fieldType")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@io.swagger.v3.oas.annotations.media.Schema(description = "Template dynamic field positioning configuration entity")
public class TemplateFieldConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID templateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FieldType fieldType;

    /**
     * Normalized X position percentage (0.0 to 100.0) relative to image width.
     */
    @Column(nullable = false)
    private Double x;

    /**
     * Normalized Y position percentage (0.0 to 100.0) relative to image height.
     */
    @Column(nullable = false)
    private Double y;

    /**
     * Normalized width percentage (0.0 to 100.0) relative to image width.
     */
    @Column(nullable = false)
    private Double width;

    /**
     * Normalized height percentage (0.0 to 100.0) relative to image height.
     */
    @Column(nullable = false)
    private Double height;

    @Builder.Default
    private Integer fontSize = 24;

    @Builder.Default
    private String fontColor = "#FFFFFF";

    @Builder.Default
    private String alignment = "CENTER"; // LEFT, CENTER, RIGHT

    @Builder.Default
    private String fontWeight = "BOLD"; // NORMAL, BOLD

    @Builder.Default
    private String fontFamily = "SansSerif";

    @Builder.Default
    private Integer qrSize = 180;

    private String sampleText;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        validateBounds();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        validateBounds();
    }

    public void validateBounds() {
        if (x == null || x < 0.0 || x > 100.0) {
            throw new IllegalArgumentException("Field position X must be between 0.0 and 100.0 percent");
        }
        if (y == null || y < 0.0 || y > 100.0) {
            throw new IllegalArgumentException("Field position Y must be between 0.0 and 100.0 percent");
        }
        if (width == null || width <= 0.0 || width > 100.0) {
            throw new IllegalArgumentException("Field width must be greater than 0.0 and up to 100.0 percent");
        }
        if (height == null || height <= 0.0 || height > 100.0) {
            throw new IllegalArgumentException("Field height must be greater than 0.0 and up to 100.0 percent");
        }
    }
}
