package com.InvitationSystem.InvitationSystem.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "receipts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Receipt {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID paymentId;

    @Column(nullable = false)
    private UUID invitationId;

    @Column(unique = true, nullable = false)
    private String receiptNumber;

    private String pdfUrl;

    private String deliveryStatus;

    private LocalDateTime generatedAt;

    @PrePersist
    public void onCreate() {
        generatedAt = LocalDateTime.now();
    }
}