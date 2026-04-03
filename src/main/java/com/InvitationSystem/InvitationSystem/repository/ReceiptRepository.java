package com.InvitationSystem.InvitationSystem.repository;

import com.InvitationSystem.InvitationSystem.entity.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {
    
    Optional<Receipt> findByInvitationId(UUID invitationId);
    
    Optional<Receipt> findByPaymentId(UUID paymentId);
    
    Optional<Receipt> findByReceiptNumber(String receiptNumber);
    
    List<Receipt> findByDeliveryStatus(String deliveryStatus);
}
