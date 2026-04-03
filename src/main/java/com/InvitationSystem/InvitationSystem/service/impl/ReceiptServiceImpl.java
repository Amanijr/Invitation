package com.InvitationSystem.InvitationSystem.service.impl;

import com.InvitationSystem.InvitationSystem.Dto.receiptsDto.ReceiptRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.receiptsDto.ReceiptResponseDto;
import com.InvitationSystem.InvitationSystem.entity.Receipt;
import com.InvitationSystem.InvitationSystem.repository.ReceiptRepository;
import com.InvitationSystem.InvitationSystem.service.ReceiptService;
import com.InvitationSystem.InvitationSystem.util.PDFService;
import com.InvitationSystem.InvitationSystem.util.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ReceiptServiceImpl implements ReceiptService {

    @Autowired
    private ReceiptRepository receiptRepository;

    @Autowired
    private PDFService pdfService;

    @Autowired
    private EmailService emailService;

    @Override
    public ReceiptResponseDto createReceipt(ReceiptRequestDto request) {
        Receipt receipt = Receipt.builder()
                .paymentId(request.getPaymentId())
                .invitationId(request.getInvitationId())
                .receiptNumber(request.getReceiptNumber())
                .deliveryStatus("PENDING")
                .build();

        Receipt savedReceipt = receiptRepository.save(receipt);
        return mapToResponseDto(savedReceipt);
    }

    @Override
    public ReceiptResponseDto getReceiptById(UUID receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new IllegalArgumentException("Receipt not found with ID: " + receiptId));
        return mapToResponseDto(receipt);
    }

    @Override
    public ReceiptResponseDto getReceiptByInvitation(UUID invitationId) {
        Receipt receipt = receiptRepository.findByInvitationId(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Receipt not found for invitation: " + invitationId));
        return mapToResponseDto(receipt);
    }

    @Override
    public ReceiptResponseDto getReceiptByPayment(UUID paymentId) {
        Receipt receipt = receiptRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Receipt not found for payment: " + paymentId));
        return mapToResponseDto(receipt);
    }

    @Override
    public List<ReceiptResponseDto> getAllReceipts() {
        return receiptRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public List<ReceiptResponseDto> getReceiptsByDeliveryStatus(String deliveryStatus) {
        return receiptRepository.findByDeliveryStatus(deliveryStatus).stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public ReceiptResponseDto updateReceiptPdf(UUID receiptId, String pdfUrl) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new IllegalArgumentException("Receipt not found with ID: " + receiptId));

        receipt.setPdfUrl(pdfUrl);
        Receipt updatedReceipt = receiptRepository.save(receipt);
        return mapToResponseDto(updatedReceipt);
    }

    @Override
    public ReceiptResponseDto updateDeliveryStatus(UUID receiptId, String deliveryStatus) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new IllegalArgumentException("Receipt not found with ID: " + receiptId));

        receipt.setDeliveryStatus(deliveryStatus);
        Receipt updatedReceipt = receiptRepository.save(receipt);
        return mapToResponseDto(updatedReceipt);
    }

    @Override
    public void deleteReceipt(UUID receiptId) {
        receiptRepository.deleteById(receiptId);
    }

    private ReceiptResponseDto mapToResponseDto(Receipt receipt) {
        ReceiptResponseDto dto = new ReceiptResponseDto();
        dto.setId(receipt.getId());
        dto.setPaymentId(receipt.getPaymentId());
        dto.setInvitationId(receipt.getInvitationId());
        dto.setReceiptNumber(receipt.getReceiptNumber());
        dto.setPdfUrl(receipt.getPdfUrl());
        dto.setDeliveryStatus(receipt.getDeliveryStatus());
        dto.setGeneratedAt(receipt.getGeneratedAt());
        return dto;
    }
}
