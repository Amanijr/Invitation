package com.InvitationSystem.InvitationSystem.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "events", indexes = {
        @Index(name = "idx_event_created_by", columnList = "createdBy"),
        @Index(name = "idx_event_status", columnList = "status"),
        @Index(name = "idx_event_date", columnList = "eventDate")
})


@io.swagger.v3.oas.annotations.media.Schema(description = "Event persistence entity")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String eventName;

    private String eventDescription;

    @Column(nullable = false)
    private String venue;

    @Column(nullable = false)
    private LocalDateTime eventDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private EventType eventType;

    private String status;

    @Column(columnDefinition = "TEXT")
    private String stayDetails;

    private String registryUrl;

    private String registryLabel;

    @Builder.Default
    private Boolean askDietary = true;

    @Builder.Default
    private Boolean askMeal = true;

    private String mealOptions;

    @Column(nullable = false)
    private UUID createdBy;

    private UUID currentTemplateId;

    @Builder.Default
    private Integer currentTemplateVersion = 1;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
