package com.InvitationSystem.InvitationSystem.repository;

import com.InvitationSystem.InvitationSystem.entity.DeliveryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryLogRepository extends JpaRepository<DeliveryLog, UUID> {
    
    List<DeliveryLog> findByInvitationId(UUID invitationId);

    List<DeliveryLog> findByInvitationIdIn(java.util.Collection<UUID> invitationIds);

    List<DeliveryLog> findByGuestId(UUID guestId);
    
    List<DeliveryLog> findByStatus(String status);
    
    List<DeliveryLog> findByChannel(String channel);
    
    List<DeliveryLog> findByInvitationIdAndChannel(UUID invitationId, String channel);
    
    List<DeliveryLog> findByStatusOrderBySentAtDesc(String status);

    Optional<DeliveryLog> findByIdempotencyKey(String idempotencyKey);

    Optional<DeliveryLog> findFirstByProviderReferenceOrderBySentAtDesc(String providerReference);

    Optional<DeliveryLog> findByInvitationIdAndChannelAndIdempotencyKey(UUID invitationId, String channel, String idempotencyKey);

    long countByChannel(String channel);

    long countByStatus(String status);

    long countByChannelAndStatus(String channel, String status);

    List<DeliveryLog> findTop50ByOrderBySentAtDesc();
}
