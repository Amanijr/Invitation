package com.InvitationSystem.InvitationSystem.repository;

import com.InvitationSystem.InvitationSystem.entity.DeliveryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeliveryLogRepository extends JpaRepository<DeliveryLog, UUID> {
    
    List<DeliveryLog> findByInvitationId(UUID invitationId);
    
    List<DeliveryLog> findByStatus(String status);
    
    List<DeliveryLog> findByChannel(String channel);
    
    List<DeliveryLog> findByInvitationIdAndChannel(UUID invitationId, String channel);
    
    List<DeliveryLog> findByStatusOrderBySentAtDesc(String status);
}
