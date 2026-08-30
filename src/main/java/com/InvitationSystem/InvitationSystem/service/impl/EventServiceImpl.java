package com.InvitationSystem.InvitationSystem.service.impl;

import com.InvitationSystem.InvitationSystem.Dto.eventsDto.EventGuestExperienceDto;
import com.InvitationSystem.InvitationSystem.Dto.eventsDto.EventRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.eventsDto.EventResponseDto;
import com.InvitationSystem.InvitationSystem.Dto.eventsDto.EventTemplateChangeRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.eventsDto.EventTemplateChangeResponseDto;
import com.InvitationSystem.InvitationSystem.entity.Event;
import com.InvitationSystem.InvitationSystem.entity.EventType;
import com.InvitationSystem.InvitationSystem.entity.Template;
import com.InvitationSystem.InvitationSystem.entity.TemplateChangeScope;
import com.InvitationSystem.InvitationSystem.entity.UserRole;
import com.InvitationSystem.InvitationSystem.repository.EventRepository;
import com.InvitationSystem.InvitationSystem.repository.TemplateRepository;
import com.InvitationSystem.InvitationSystem.security.EventAuthorization;
import com.InvitationSystem.InvitationSystem.service.EventService;
import com.InvitationSystem.InvitationSystem.service.InvitationService;
import com.InvitationSystem.InvitationSystem.util.TemplateAvailability;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class EventServiceImpl implements EventService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private InvitationService invitationService;

    @Override
    public EventResponseDto createEvent(EventRequestDto request, UUID createdBy) {
        Event event = Event.builder()
                .eventName(request.getEventName())
                .eventDescription(request.getEventDescription())
                .venue(request.getVenue() == null || request.getVenue().isBlank()
                        ? "To be confirmed"
                        : request.getVenue().trim())
                .eventDate(request.getEventDate() != null
                        ? request.getEventDate()
                        : LocalDateTime.now().plusWeeks(2).withHour(18).withMinute(0).withSecond(0).withNano(0))
                .eventType(request.getEventType())
                .status(normalizeStatus(request.getStatus()))
                .stayDetails(request.getStayDetails())
                .registryUrl(request.getRegistryUrl())
                .registryLabel(request.getRegistryLabel())
                .askDietary(request.getAskDietary() == null || request.getAskDietary())
                .askMeal(request.getAskMeal() == null || request.getAskMeal())
                .mealOptions(request.getMealOptions())
                .createdBy(createdBy)
                .build();

        if (request.getCurrentTemplateId() != null) {
            Template template = requireTemplateForEvent(request.getCurrentTemplateId(), null);
            event.setCurrentTemplateId(template.getId());
            event.setCurrentTemplateVersion(template.resolvedVersion());
        }

        Event savedEvent = eventRepository.save(event);
        return mapToResponseDto(savedEvent);
    }

    @Override
    public EventResponseDto getEventById(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + eventId));
        return mapToResponseDto(event);
    }

    @Override
    public void assertCanAccess(UUID eventId, UUID actorId, UserRole actorRole) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + eventId));
        EventAuthorization.requireEventOwnerOrAdmin(event, actorId, actorRole);
    }

    @Override
    public List<UUID> visibleEventIds(UUID actorId, UserRole actorRole) {
        if (actorRole == UserRole.ADMIN) {
            return eventRepository.findAll().stream().map(Event::getId).toList();
        }
        if (actorId == null) {
            return List.of();
        }
        return eventRepository.findByCreatedBy(actorId).stream().map(Event::getId).toList();
    }

    @Override
    public List<EventResponseDto> getEventsByCreator(UUID createdBy) {
        return eventRepository.findByCreatedByOrderByCreatedAtDesc(createdBy).stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public List<EventResponseDto> getAllEvents() {
        return eventRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public List<EventResponseDto> getEventsByType(EventType eventType) {
        return eventRepository.findByEventTypeOrderByCreatedAtDesc(eventType).stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public EventResponseDto updateEvent(UUID eventId, EventRequestDto request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + eventId));

        event.setEventName(request.getEventName());
        event.setEventDescription(request.getEventDescription());
        event.setVenue(request.getVenue());
        event.setEventDate(request.getEventDate());
        
        // Update eventType - defensive check for backwards compatibility
        if (request.getEventType() != null) {
            event.setEventType(request.getEventType());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            event.setStatus(normalizeStatus(request.getStatus()));
        }

        applyGuestExperience(
                event,
                request.getStayDetails(),
                request.getRegistryUrl(),
                request.getRegistryLabel(),
                request.getAskDietary(),
                request.getAskMeal(),
                request.getMealOptions(),
                false);

        Event updatedEvent = eventRepository.save(event);
        return mapToResponseDto(updatedEvent);
    }

    @Override
    public EventResponseDto updateGuestExperience(UUID eventId, EventGuestExperienceDto request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + eventId));
        applyGuestExperience(
                event,
                request.getStayDetails(),
                request.getRegistryUrl(),
                request.getRegistryLabel(),
                request.getAskDietary(),
                request.getAskMeal(),
                request.getMealOptions(),
                true);
        return mapToResponseDto(eventRepository.save(event));
    }

    @Override
    public EventResponseDto updateEventStatus(UUID eventId, String status) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + eventId));

        event.setStatus(status);
        Event updatedEvent = eventRepository.save(event);
        return mapToResponseDto(updatedEvent);
    }

    @Override
    @Transactional
    public EventTemplateChangeResponseDto assignCurrentTemplate(
            UUID eventId,
            EventTemplateChangeRequestDto request,
            UUID actorId,
            UserRole actorRole) {
        if (request == null || request.getTemplateId() == null) {
            throw new IllegalArgumentException("templateId is required");
        }
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + eventId));
        EventAuthorization.requireEventOwnerOrAdmin(event, actorId, actorRole);

        Template template = requireTemplateForEvent(request.getTemplateId(), event.getId());
        TemplateChangeScope scope = request.getScope() == null
                ? TemplateChangeScope.NEW_GUESTS_ONLY
                : request.getScope();

        if (scope == TemplateChangeScope.ALL_INVITATIONS && !Boolean.TRUE.equals(request.getConfirm())) {
            throw new IllegalArgumentException(
                    "Changing the template for all invitations will regenerate existing invitations. Set confirm=true to proceed.");
        }

        event.setCurrentTemplateId(template.getId());
        event.setCurrentTemplateVersion(template.resolvedVersion());
        Event saved = eventRepository.save(event);

        int regenerated = invitationService.regenerateInvitationsForTemplateChange(
                eventId, template.getId(), template.resolvedVersion(), scope);

        String message = switch (scope) {
            case NEW_GUESTS_ONLY -> "New guests will use this template. Existing invitations were left unchanged.";
            case UNSENT_INVITATIONS -> "Unsent invitations were regenerated. Sent invitations were left unchanged.";
            case ALL_INVITATIONS -> "Existing invitations were regenerated onto the new template.";
        };

        return EventTemplateChangeResponseDto.builder()
                .event(mapToResponseDto(saved))
                .scope(scope)
                .regeneratedCount(regenerated)
                .skippedCount(0)
                .message(message)
                .build();
    }

    @Override
    public void deleteEvent(UUID eventId) {
        eventRepository.deleteById(eventId);
    }

    private EventResponseDto mapToResponseDto(Event event) {
        EventResponseDto dto = new EventResponseDto();
        dto.setId(event.getId());
        dto.setEventName(event.getEventName());
        dto.setEventDescription(event.getEventDescription());
        dto.setVenue(event.getVenue());
        dto.setEventDate(event.getEventDate());
        dto.setEventType(event.getEventType() != null ? event.getEventType() : null);
        dto.setStatus(event.getStatus());
        dto.setStayDetails(event.getStayDetails());
        dto.setRegistryUrl(event.getRegistryUrl());
        dto.setRegistryLabel(event.getRegistryLabel());
        dto.setAskDietary(event.getAskDietary() == null || event.getAskDietary());
        dto.setAskMeal(event.getAskMeal() == null || event.getAskMeal());
        dto.setMealOptions(event.getMealOptions());
        dto.setCurrentTemplateId(event.getCurrentTemplateId());
        dto.setCurrentTemplateVersion(event.getCurrentTemplateVersion());
        return dto;
    }

    private Template requireTemplateForEvent(UUID templateId, UUID eventId) {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found with ID: " + templateId));
        if (!template.isActive()) {
            throw new IllegalArgumentException("Template is not active");
        }
        if (eventId != null && !TemplateAvailability.isAvailableForEvent(template, eventId)) {
            throw new IllegalArgumentException("Template is not available for this event");
        }
        return template;
    }

    private void applyGuestExperience(
            Event event,
            String stayDetails,
            String registryUrl,
            String registryLabel,
            Boolean askDietary,
            Boolean askMeal,
            String mealOptions,
            boolean replace) {
        if (replace || stayDetails != null) {
            event.setStayDetails(blankToNull(stayDetails));
        }
        if (replace || registryUrl != null) {
            event.setRegistryUrl(blankToNull(registryUrl));
        }
        if (replace || registryLabel != null) {
            event.setRegistryLabel(blankToNull(registryLabel));
        }
        if (askDietary != null) {
            event.setAskDietary(askDietary);
        }
        if (askMeal != null) {
            event.setAskMeal(askMeal);
        }
        if (replace || mealOptions != null) {
            event.setMealOptions(blankToNull(mealOptions));
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "ACTIVE";
        }
        return status.trim().toUpperCase();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
