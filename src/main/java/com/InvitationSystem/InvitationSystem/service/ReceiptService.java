package com.InvitationSystem.InvitationSystem.service;

import com.InvitationSystem.InvitationSystem.Dto.receiptsDto.ReceiptRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.receiptsDto.ReceiptResponseDto;

import java.util.List;
import java.util.UUID;

public interface ReceiptService {

    ReceiptResponseDto createReceipt(ReceiptRequestDto request);

    ReceiptResponseDto getReceiptById(UUID receiptId);

    ReceiptResponseDto getReceiptByInvitation(UUID invitationId);

    ReceiptResponseDto getReceiptByPayment(UUID paymentId);

    List<ReceiptResponseDto> getAllReceipts();

    List<ReceiptResponseDto> getReceiptsByDeliveryStatus(String deliveryStatus);

    ReceiptResponseDto updateReceiptPdf(UUID receiptId, String pdfUrl);

    ReceiptResponseDto updateDeliveryStatus(UUID receiptId, String deliveryStatus);

    void deleteReceipt(UUID receiptId);
}
