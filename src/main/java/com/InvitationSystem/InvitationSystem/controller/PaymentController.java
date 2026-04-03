package com.InvitationSystem.InvitationSystem.controller;

import com.InvitationSystem.InvitationSystem.Dto.paymentsDto.PaymentRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.paymentsDto.PaymentResponseDto;
import com.InvitationSystem.InvitationSystem.service.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@CrossOrigin(origins = "*")
@Tag(name = "Payments", description = "Payment processing endpoints")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponseDto> createPayment(@RequestBody PaymentRequestDto request) {
        PaymentResponseDto response = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponseDto> getPaymentById(@PathVariable UUID paymentId) {
        PaymentResponseDto response = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/invitation/{invitationId}")
    public ResponseEntity<PaymentResponseDto> getPaymentByInvitation(@PathVariable UUID invitationId) {
        PaymentResponseDto response = paymentService.getPaymentByInvitation(invitationId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<PaymentResponseDto>> getAllPayments() {
        List<PaymentResponseDto> payments = paymentService.getAllPayments();
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<PaymentResponseDto>> getPaymentsByStatus(@PathVariable String status) {
        List<PaymentResponseDto> payments = paymentService.getPaymentsByStatus(status);
        return ResponseEntity.ok(payments);
    }

    @PatchMapping("/{paymentId}/complete")
    public ResponseEntity<PaymentResponseDto> completePayment(@PathVariable UUID paymentId, @RequestParam String transactionReference) {
        PaymentResponseDto response = paymentService.completePayment(paymentId, transactionReference);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{paymentId}/fail")
    public ResponseEntity<PaymentResponseDto> failPayment(@PathVariable UUID paymentId) {
        PaymentResponseDto response = paymentService.failPayment(paymentId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{paymentId}")
    public ResponseEntity<Void> deletePayment(@PathVariable UUID paymentId) {
        paymentService.deletePayment(paymentId);
        return ResponseEntity.noContent().build();
    }
}
