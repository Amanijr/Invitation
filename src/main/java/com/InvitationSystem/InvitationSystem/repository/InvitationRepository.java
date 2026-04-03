package com.InvitationSystem.InvitationSystem.repository;

import com.InvitationSystem.InvitationSystem.entity.Invitation;
import com.InvitationSystem.InvitationSystem.entity.InvitationStatus;
import com.InvitationSystem.InvitationSystem.entity.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, UUID> {
    
    Optional<Invitation> findByUniqueToken(String uniqueToken);

    boolean existsByUniqueToken(String uniqueToken);
    
    Optional<Invitation> findByEventIdAndGuestId(UUID eventId, UUID guestId);
    
    List<Invitation> findByEventId(UUID eventId);
    
    List<Invitation> findByGuestId(UUID guestId);
    
    List<Invitation> findByStatus(InvitationStatus status);

    List<Invitation> findByDeliveryStatus(DeliveryStatus deliveryStatus);
    
    List<Invitation> findByBulkUploadSessionId(UUID bulkUploadSessionId);
    
    List<Invitation> findByEventIdAndStatus(UUID eventId, InvitationStatus status);
    
    int countByBulkUploadSessionId(UUID bulkUploadSessionId);
}
