package com.InvitationSystem.InvitationSystem.Dto.paymentsDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@io.swagger.v3.oas.annotations.media.Schema(description = "Response payload for payment details")
public class PaymentResponseDto {

    private UUID id;
    private UUID invitationId;
    private Double amount;
    private String currency;
    private String paymentMethod;
    private String transactionReference;
    private String status;
    private LocalDateTime paidAt;
}
