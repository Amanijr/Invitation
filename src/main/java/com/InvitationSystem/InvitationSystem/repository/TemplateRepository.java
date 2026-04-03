package com.InvitationSystem.InvitationSystem.repository;

import com.InvitationSystem.InvitationSystem.entity.EventType;
import com.InvitationSystem.InvitationSystem.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TemplateRepository extends JpaRepository<Template, UUID> {


    List<Template> findByEventType(EventType eventType);
    List<Template> findByEventTypeAndActive(EventType eventType, boolean active);
    Optional<Template> findByEventIdAndTemplateName(UUID eventId, String templateName);


    List<Template> findByActive(boolean active);
    List<Template> findByTemplateNameContainingIgnoreCase(String name);
    List<Template> findByEventTypeAndTemplateNameContainingIgnoreCase(EventType eventType, String name);


    long countByEventType(EventType eventType);
    long countByEventTypeAndActive(EventType eventType, boolean active);


    boolean existsByEventIdAndTemplateName(UUID eventId, String templateName);
    boolean existsByEventTypeAndActive(EventType eventType, boolean active);


    List<Template> findByEventId(UUID eventId);
    List<Template> findByEventIdAndActive(UUID eventId, boolean active);
}