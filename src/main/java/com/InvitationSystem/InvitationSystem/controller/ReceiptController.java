package com.InvitationSystem.InvitationSystem.controller;

import com.InvitationSystem.InvitationSystem.Dto.receiptsDto.ReceiptRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.receiptsDto.ReceiptResponseDto;
import com.InvitationSystem.InvitationSystem.service.ReceiptService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/receipts")
@CrossOrigin(origins = "*")
@Tag(name = "Receipts", description = "Receipt generation and delivery tracking endpoints")
public class ReceiptController {

    @Autowired
    private ReceiptService receiptService;

    @PostMapping
    public ResponseEntity<ReceiptResponseDto> createReceipt(@RequestBody ReceiptRequestDto request) {
        ReceiptResponseDto response = receiptService.createReceipt(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{receiptId}")
    public ResponseEntity<ReceiptResponseDto> getReceiptById(@PathVariable UUID receiptId) {
        ReceiptResponseDto response = receiptService.getReceiptById(receiptId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/invitation/{invitationId}")
    public ResponseEntity<ReceiptResponseDto> getReceiptByInvitation(@PathVariable UUID invitationId) {
        ReceiptResponseDto response = receiptService.getReceiptByInvitation(invitationId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<ReceiptResponseDto> getReceiptByPayment(@PathVariable UUID paymentId) {
        ReceiptResponseDto response = receiptService.getReceiptByPayment(paymentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ReceiptResponseDto>> getAllReceipts() {
        List<ReceiptResponseDto> receipts = receiptService.getAllReceipts();
        return ResponseEntity.ok(receipts);
    }

    @GetMapping("/delivery-status/{deliveryStatus}")
    public ResponseEntity<List<ReceiptResponseDto>> getReceiptsByDeliveryStatus(@PathVariable String deliveryStatus) {
        List<ReceiptResponseDto> receipts = receiptService.getReceiptsByDeliveryStatus(deliveryStatus);
        return ResponseEntity.ok(receipts);
    }

    @PatchMapping("/{receiptId}/pdf")
    public ResponseEntity<ReceiptResponseDto> updateReceiptPdf(@PathVariable UUID receiptId, @RequestParam String pdfUrl) {
        ReceiptResponseDto response = receiptService.updateReceiptPdf(receiptId, pdfUrl);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{receiptId}/delivery-status")
    public ResponseEntity<ReceiptResponseDto> updateDeliveryStatus(@PathVariable UUID receiptId, @RequestParam String deliveryStatus) {
        ReceiptResponseDto response = receiptService.updateDeliveryStatus(receiptId, deliveryStatus);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{receiptId}")
    public ResponseEntity<Void> deleteReceipt(@PathVariable UUID receiptId) {
        receiptService.deleteReceipt(receiptId);
        return ResponseEntity.noContent().build();
    }
}
