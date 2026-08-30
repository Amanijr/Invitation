package com.InvitationSystem.InvitationSystem.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invitations",
        uniqueConstraints = @UniqueConstraint(name = "uk_invitation_event_guest", columnNames = {"eventId", "guestId"}),
        indexes = {
                @Index(name = "idx_invitation_unique_token", columnList = "uniqueToken", unique = true),
                @Index(name = "idx_invitation_event_id", columnList = "eventId"),
                @Index(name = "idx_invitation_guest_id", columnList = "guestId"),
                @Index(name = "idx_invitation_status", columnList = "status")
        })
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

    @Builder.Default
    @Column(nullable = false)
    private Integer templateVersion = 1;

    @Column(nullable = false)
    private UUID guestId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 16)
    private AdmissionType admissionType = AdmissionType.SINGLE;

    @Builder.Default
    @Column(nullable = false)
    private Integer admissionLimit = 1;

    @Builder.Default
    @Column(nullable = false)
    private Integer usedAdmissions = 0;

    @Builder.Default
    @Column(nullable = false)
    private boolean revoked = false;

    private LocalDateTime revokedAt;

    private String recipientPhone;

    private String recipientEmail;

    @Column(nullable = false, unique = true)
    private String uniqueToken;

    private String qrCodeUrl;

    @Lob
    private String qrCode;

    private String cardReference;

    @Builder.Default
    private boolean used = false;

    @Builder.Default
    private boolean scanned = false;

    private LocalDateTime scannedAt;

    private LocalDateTime usedAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private InvitationStatus status = InvitationStatus.GENERATED;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "delivery_status", nullable = false, length = 32)
    private DeliveryStatus deliveryStatus = DeliveryStatus.PENDING;

    private LocalDateTime sentAt;

    private LocalDateTime deliveredAt;

    private LocalDateTime openedAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(length = 32)
    private RsvpStatus rsvpStatus = RsvpStatus.NO_REPLY;

    private LocalDateTime rsvpAt;

    @Builder.Default
    private Integer partySize = 1;

    private String dietaryNotes;

    private String mealChoice;

    private LocalDateTime generatedAt;

    private LocalDateTime expiryDate;

    private LocalDateTime expiresAt;

    private UUID bulkUploadSessionId;

    @PrePersist
    public void onCreate() {
        if (generatedAt == null) {
            generatedAt = LocalDateTime.now();
        }
        if (admissionType == null) {
            admissionType = AdmissionType.SINGLE;
        }
        if (admissionLimit == null || admissionLimit < 1) {
            admissionLimit = admissionType.getAdmissionLimit();
        }
        if (usedAdmissions == null || usedAdmissions < 0) {
            usedAdmissions = 0;
        }
        if (templateVersion == null || templateVersion < 1) {
            templateVersion = 1;
        }
    }

    public int resolvedAdmissionLimit() {
        if (admissionLimit != null && admissionLimit > 0) {
            return admissionLimit;
        }
        return AdmissionType.fromNullable(admissionType).getAdmissionLimit();
    }

    public int resolvedUsedAdmissions() {
        if (usedAdmissions != null) {
            return usedAdmissions;
        }
        return used ? resolvedAdmissionLimit() : 0;
    }

    public int remainingAdmissions() {
        return Math.max(0, resolvedAdmissionLimit() - resolvedUsedAdmissions());
    }

    public CheckInEntitlementState entitlementState() {
        if (revoked) {
            return CheckInEntitlementState.REVOKED;
        }
        int usedCount = resolvedUsedAdmissions();
        int limit = resolvedAdmissionLimit();
        if (usedCount >= limit) {
            return CheckInEntitlementState.FULLY_USED;
        }
        if (usedCount > 0) {
            return CheckInEntitlementState.PARTIALLY_USED;
        }
        return CheckInEntitlementState.VALID;
    }
}