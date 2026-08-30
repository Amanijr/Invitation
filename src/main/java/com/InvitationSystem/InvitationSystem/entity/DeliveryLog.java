package com.InvitationSystem.InvitationSystem.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "delivery_logs", indexes = {
        @Index(name = "idx_delivery_log_invitation_id", columnList = "invitationId"),
        @Index(name = "idx_delivery_log_guest_id", columnList = "guestId"),
        @Index(name = "idx_delivery_log_idempotency_key", columnList = "idempotencyKey"),
        @Index(name = "idx_delivery_log_status", columnList = "status"),
        @Index(name = "idx_delivery_log_channel", columnList = "channel")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@io.swagger.v3.oas.annotations.media.Schema(description = "Delivery log persistence entity")
public class DeliveryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID invitationId;

    private UUID guestId;

    @Column(nullable = false)
    private String channel;

    @Column(nullable = false)
    private String status;

    private String recipientContact;

    private String providerReference;

    @Column(columnDefinition = "TEXT")
    private String providerResponse;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private String idempotencyKey;

    @Builder.Default
    private int retryCount = 0;

    private LocalDateTime sentAt;

    private LocalDateTime deliveredAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.sentAt == null) {
            this.sentAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}