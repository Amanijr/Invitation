package com.InvitationSystem.InvitationSystem.Dto.receiptsDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@io.swagger.v3.oas.annotations.media.Schema(description = "Response payload for receipt details")
public class ReceiptResponseDto {

    private UUID id;
    private UUID paymentId;
    private UUID invitationId;
    private String receiptNumber;
    private String pdfUrl;
    private String deliveryStatus;
    private LocalDateTime generatedAt;
}
