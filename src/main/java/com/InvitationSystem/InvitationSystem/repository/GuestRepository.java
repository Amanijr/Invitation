package com.InvitationSystem.InvitationSystem.repository;

import com.InvitationSystem.InvitationSystem.entity.Guest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GuestRepository extends JpaRepository<Guest, UUID> {
    List<Guest> findByEventId(UUID eventId);
    List<Guest> findByEventIdIn(java.util.Collection<UUID> eventIds);
    Optional<Guest> findByEventIdAndEmail(UUID eventId, String email);
    Optional<Guest> findByEventIdAndPhone(UUID eventId, String phone);
    boolean existsByEventIdAndEmail(UUID eventId, String email);
    boolean existsByEventIdAndPhone(UUID eventId, String phone);
    boolean existsByEventIdAndFullName(UUID eventId, String fullName);
    long countByEventId(UUID eventId);
    long countByEventIdIn(java.util.Collection<UUID> eventIds);

    @Query("SELECT g FROM Guest g WHERE g.eventId = :eventId AND " +
           "(LOWER(g.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(g.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "g.phone LIKE CONCAT('%', :query, '%'))")
    List<Guest> searchGuests(@Param("eventId") UUID eventId, @Param("query") String query);
}
