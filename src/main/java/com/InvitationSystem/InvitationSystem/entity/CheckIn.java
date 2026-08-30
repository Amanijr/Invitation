package com.InvitationSystem.InvitationSystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "check_ins", indexes = {
        @Index(name = "idx_checkin_invitation_id", columnList = "invitationId"),
        @Index(name = "idx_checkin_event_id", columnList = "eventId"),
        @Index(name = "idx_checkin_scanned_at", columnList = "scannedAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@io.swagger.v3.oas.annotations.media.Schema(description = "Check-in audit log persistence entity")
public class CheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "check_in_id")
    private UUID id;

    @Column(name = "invitation_id", nullable = true)
    private UUID invitationId;

    @Column(name = "event_id", nullable = true)
    private UUID eventId;

    @Column(nullable = false)
    private LocalDateTime scannedAt;

    @Column(name = "scanner_label", length = 128)
    private String scannerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CheckInResult result;

    private String notes;

    @PrePersist
    public void onCreate() {
        if (this.scannedAt == null) {
            this.scannedAt = LocalDateTime.now();
        }
    }
}
