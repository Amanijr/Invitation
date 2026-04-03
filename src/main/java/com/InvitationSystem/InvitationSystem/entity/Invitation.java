package com.InvitationSystem.InvitationSystem.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invitations",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"eventId", "guestId"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@io.swagger.v3.oas.annotations.media.Schema(description = "Invitation persistence entity")
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID eventId;

    @Column(nullable = false)
    private UUID templateId;

    @Column(nullable = false)
    private UUID guestId;

    private String recipientPhone;

    private String recipientEmail;

    @Column(nullable = false, unique = true)
    private String uniqueToken;

    private String qrCodeUrl;

    @Lob
    private String qrCode;

    @Builder.Default
    private boolean used = false;

    @Builder.Default
    private boolean scanned = false;

    private LocalDateTime scannedAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private InvitationStatus status = InvitationStatus.GENERATED;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private DeliveryStatus deliveryStatus = DeliveryStatus.PENDING;

    private LocalDateTime sentAt;

    private LocalDateTime deliveredAt;

    private LocalDateTime openedAt;

    private LocalDateTime generatedAt;

    private LocalDateTime expiryDate;

    private LocalDateTime expiresAt;

    private UUID bulkUploadSessionId;

    @PrePersist
    public void onCreate() {
        generatedAt = LocalDateTime.now();
    }
}