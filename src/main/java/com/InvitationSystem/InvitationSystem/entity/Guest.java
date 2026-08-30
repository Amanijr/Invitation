package com.InvitationSystem.InvitationSystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "guests", indexes = {
        @Index(name = "idx_guest_event_id", columnList = "eventId"),
        @Index(name = "idx_guest_email", columnList = "email"),
        @Index(name = "idx_guest_phone", columnList = "phone")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@io.swagger.v3.oas.annotations.media.Schema(description = "Guest persistence entity")
public class Guest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "guest_id")
    private UUID id;

    @Column(nullable = false)
    private UUID eventId;

    @Column(nullable = false)
    private String fullName;

    private String phone;

    private String email;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
