package com.InvitationSystem.InvitationSystem.service.impl;

import com.InvitationSystem.InvitationSystem.Dto.paymentsDto.PaymentRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.paymentsDto.PaymentResponseDto;
import com.InvitationSystem.InvitationSystem.entity.Payment;
import com.InvitationSystem.InvitationSystem.repository.PaymentRepository;
import com.InvitationSystem.InvitationSystem.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Override
    public PaymentResponseDto createPayment(PaymentRequestDto request) {
        Payment payment = Payment.builder()
                .invitationId(request.getInvitationId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .status("PENDING")
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        return mapToResponseDto(savedPayment);
    }

    @Override
    public PaymentResponseDto getPaymentById(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found with ID: " + paymentId));
        return mapToResponseDto(payment);
    }

    @Override
    public PaymentResponseDto getPaymentByInvitation(UUID invitationId) {
        Payment payment = paymentRepository.findByInvitationId(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for invitation: " + invitationId));
        return mapToResponseDto(payment);
    }

    @Override
    public List<PaymentResponseDto> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public List<PaymentResponseDto> getPaymentsByStatus(String status) {
        return paymentRepository.findByStatusOrderByPaidAtDesc(status).stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public PaymentResponseDto completePayment(UUID paymentId, String transactionReference) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found with ID: " + paymentId));

        payment.setStatus("COMPLETED");
        payment.setTransactionReference(transactionReference);
        payment.setPaidAt(LocalDateTime.now());

        Payment updatedPayment = paymentRepository.save(payment);
        return mapToResponseDto(updatedPayment);
    }

    @Override
    public PaymentResponseDto failPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found with ID: " + paymentId));

        payment.setStatus("FAILED");
        Payment updatedPayment = paymentRepository.save(payment);
        return mapToResponseDto(updatedPayment);
    }

    @Override
    public void deletePayment(UUID paymentId) {
        paymentRepository.deleteById(paymentId);
    }

    private PaymentResponseDto mapToResponseDto(Payment payment) {
        PaymentResponseDto dto = new PaymentResponseDto();
        dto.setId(payment.getId());
        dto.setInvitationId(payment.getInvitationId());
        dto.setAmount(payment.getAmount());
        dto.setCurrency(payment.getCurrency());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setTransactionReference(payment.getTransactionReference());
        dto.setStatus(payment.getStatus());
        dto.setPaidAt(payment.getPaidAt());
        return dto;
    }
}
