package com.InvitationSystem.InvitationSystem.repository;

import com.InvitationSystem.InvitationSystem.entity.Event;
import com.InvitationSystem.InvitationSystem.entity.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
    
    List<Event> findByCreatedBy(UUID createdBy);

    List<Event> findByCreatedByOrderByCreatedAtDesc(UUID createdBy);
    
    List<Event> findByStatus(String status);

    List<Event> findByEventTypeOrderByCreatedAtDesc(EventType eventType);
}
