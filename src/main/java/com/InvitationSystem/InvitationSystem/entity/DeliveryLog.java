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
public class DeliveryLog {

    @Id
    @GeneratedValue
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