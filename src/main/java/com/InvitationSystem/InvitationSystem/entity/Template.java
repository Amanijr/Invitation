package com.InvitationSystem.InvitationSystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "templates", indexes = {
        @Index(name = "idx_template_event_id", columnList = "eventId"),
        @Index(name = "idx_template_event_type", columnList = "eventType")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@io.swagger.v3.oas.annotations.media.Schema(description = "Template persistence entity")
public class Template {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private EventType eventType;

    @Column(nullable = false)
    private UUID eventId;

    @Column(nullable = false)
    private String templateName;

    private String originalFileName;

    private String storagePath;

    private String mimeType;

    private Long fileSize;

    @Lob
    private String content;

    private String previewImageUrl;

    @Builder.Default
    private Integer width = 1920;

    @Builder.Default
    private Integer height = 1080;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    @Column(nullable = false)
    private Integer version = 1;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        if (version == null || version < 1) {
            version = 1;
        }
    }

    public int resolvedVersion() {
        return version == null || version < 1 ? 1 : version;
    }

    public int bumpVersion() {
        version = resolvedVersion() + 1;
        return version;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
