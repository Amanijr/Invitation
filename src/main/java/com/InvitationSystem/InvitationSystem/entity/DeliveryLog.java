package com.InvitationSystem.InvitationSystem.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "delivery_logs")
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

    @Column(nullable = false)
    private String channel;

    @Column(nullable = false)
    private String status;

    private String providerResponse;

    private int retryCount;

    private LocalDateTime sentAt;

    private LocalDateTime deliveredAt;
}