package com.InvitationSystem.InvitationSystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bulk_upload_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@io.swagger.v3.oas.annotations.media.Schema(description = "Bulk upload session persistence entity")
public class BulkUploadSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID eventId;

    @Column(nullable = false)
    private UUID uploadedBy;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileType;

    private String fileUrl;

    @Column(nullable = false)
    private int totalGuests;

    private int successCount = 0;

    private int failureCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UploadStatus status = UploadStatus.PENDING;

    private String errorMessage;

    private LocalDateTime uploadedAt;

    private LocalDateTime processedAt;

    @PrePersist
    public void onCreate() {
        uploadedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        processedAt = LocalDateTime.now();
    }
}
