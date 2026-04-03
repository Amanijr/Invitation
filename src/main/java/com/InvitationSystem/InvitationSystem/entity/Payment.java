package com.InvitationSystem.InvitationSystem.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@io.swagger.v3.oas.annotations.media.Schema(description = "Payment persistence entity")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID invitationId;

    private Double amount;

    private String currency;

    private String paymentMethod;

    @Column(unique = true)
    private String transactionReference;

    private String status;

    private LocalDateTime paidAt;
}