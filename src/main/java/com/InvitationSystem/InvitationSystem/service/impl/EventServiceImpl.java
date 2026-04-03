package com.InvitationSystem.InvitationSystem.service.impl;

import com.InvitationSystem.InvitationSystem.Dto.eventsDto.EventRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.eventsDto.EventResponseDto;
import com.InvitationSystem.InvitationSystem.entity.Event;
import com.InvitationSystem.InvitationSystem.entity.EventType;
import com.InvitationSystem.InvitationSystem.repository.EventRepository;
import com.InvitationSystem.InvitationSystem.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EventServiceImpl implements EventService {

    @Autowired
    private EventRepository eventRepository;

    @Override
    public EventResponseDto createEvent(EventRequestDto request, UUID createdBy) {
        Event event = Event.builder()
                .eventName(request.getEventName())
                .eventDescription(request.getEventDescription())
                .venue(request.getVenue())
                .eventDate(request.getEventDate())
                .eventType(request.getEventType())
                .status("ACTIVE")
                .createdBy(createdBy)
                .build();

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

        Event updatedEvent = eventRepository.save(event);
        return mapToResponseDto(updatedEvent);
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
        return dto;
    }
}
