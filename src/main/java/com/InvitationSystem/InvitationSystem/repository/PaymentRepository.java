package com.InvitationSystem.InvitationSystem.repository;

import com.InvitationSystem.InvitationSystem.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    
    Optional<Payment> findByInvitationId(UUID invitationId);
    
    List<Payment> findByStatus(String status);
    
    Optional<Payment> findByTransactionReference(String transactionReference);
    
    List<Payment> findByStatusOrderByPaidAtDesc(String status);
}
