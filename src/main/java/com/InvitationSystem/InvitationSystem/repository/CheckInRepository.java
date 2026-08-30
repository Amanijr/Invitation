package com.InvitationSystem.InvitationSystem.repository;

import com.InvitationSystem.InvitationSystem.entity.CheckIn;
import com.InvitationSystem.InvitationSystem.entity.CheckInResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CheckInRepository extends JpaRepository<CheckIn, UUID> {
    List<CheckIn> findByEventId(UUID eventId);
    List<CheckIn> findByEventIdInOrderByScannedAtDesc(java.util.Collection<UUID> eventIds);
    List<CheckIn> findByInvitationId(UUID invitationId);
    List<CheckIn> findByEventIdAndResult(UUID eventId, CheckInResult result);
    long countByEventIdAndResult(UUID eventId, CheckInResult result);
    List<CheckIn> findTop100ByOrderByScannedAtDesc();
}
