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
public class Invitation {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID eventId;

    @Column(nullable = false)
    private UUID templateId;

    @Column(nullable = false)
    private UUID guestId;

    @Column(nullable = false, unique = true)
    private String uniqueToken;

    private String qrCodeUrl;

    private boolean isUsed = false;

    private LocalDateTime generatedAt;

    private LocalDateTime expiryDate;

    @PrePersist
    public void onCreate() {
        generatedAt = LocalDateTime.now();
    }
}