package com.InvitationSystem.InvitationSystem.Dto.paymentsDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@io.swagger.v3.oas.annotations.media.Schema(description = "Request payload for creating a payment")
public class PaymentRequestDto {

    private UUID invitationId;
    private Double amount;
    private String currency;
    private String paymentMethod;
}
