package com.InvitationSystem.InvitationSystem.repository;

import com.InvitationSystem.InvitationSystem.entity.Invitation;
import com.InvitationSystem.InvitationSystem.entity.InvitationStatus;
import com.InvitationSystem.InvitationSystem.entity.DeliveryStatus;
import com.InvitationSystem.InvitationSystem.entity.RsvpStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, UUID> {
    
    Optional<Invitation> findByUniqueToken(String uniqueToken);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Invitation i WHERE i.uniqueToken = :uniqueToken")
    Optional<Invitation> findByUniqueTokenForUpdate(@Param("uniqueToken") String uniqueToken);

    boolean existsByUniqueToken(String uniqueToken);
    
    Optional<Invitation> findByEventIdAndGuestId(UUID eventId, UUID guestId);
    
    List<Invitation> findByEventId(UUID eventId);

    List<Invitation> findByEventIdIn(java.util.Collection<UUID> eventIds);

    List<Invitation> findByEventIdAndDeliveryStatusIn(UUID eventId, java.util.Collection<DeliveryStatus> statuses);
    
    List<Invitation> findByGuestId(UUID guestId);
    
    List<Invitation> findByStatus(InvitationStatus status);

    List<Invitation> findByDeliveryStatus(DeliveryStatus deliveryStatus);
    
    List<Invitation> findByBulkUploadSessionId(UUID bulkUploadSessionId);
    
    List<Invitation> findByEventIdAndStatus(UUID eventId, InvitationStatus status);
    
    int countByBulkUploadSessionId(UUID bulkUploadSessionId);

    long countByEventId(UUID eventId);

    long countByEventIdIn(java.util.Collection<UUID> eventIds);

    long countByEventIdAndUsedTrue(UUID eventId);

    long countByEventIdInAndUsedTrue(java.util.Collection<UUID> eventIds);

    long countByStatus(InvitationStatus status);

    long countByUsedTrue();

    long countByUsedFalse();

    long countByEventIdAndRsvpStatus(UUID eventId, RsvpStatus rsvpStatus);

    long countByEventIdAndOpenedAtIsNotNull(UUID eventId);
}
