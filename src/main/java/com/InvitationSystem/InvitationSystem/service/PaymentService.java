package com.InvitationSystem.InvitationSystem.service;

import com.InvitationSystem.InvitationSystem.Dto.paymentsDto.PaymentRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.paymentsDto.PaymentResponseDto;

import java.util.List;
import java.util.UUID;

public interface PaymentService {

    PaymentResponseDto createPayment(PaymentRequestDto request);

    PaymentResponseDto getPaymentById(UUID paymentId);

    PaymentResponseDto getPaymentByInvitation(UUID invitationId);

    List<PaymentResponseDto> getAllPayments();

    List<PaymentResponseDto> getPaymentsByStatus(String status);

    PaymentResponseDto completePayment(UUID paymentId, String transactionReference);

    PaymentResponseDto failPayment(UUID paymentId);

    void deletePayment(UUID paymentId);
}
