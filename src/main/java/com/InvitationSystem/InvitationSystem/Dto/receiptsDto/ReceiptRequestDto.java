package com.InvitationSystem.InvitationSystem.Dto.receiptsDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@io.swagger.v3.oas.annotations.media.Schema(description = "Request payload for creating a receipt")
public class ReceiptRequestDto {

    private UUID paymentId;
    private UUID invitationId;
    private String receiptNumber;
}
