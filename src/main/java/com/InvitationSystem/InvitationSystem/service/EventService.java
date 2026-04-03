package com.InvitationSystem.InvitationSystem.service;

import com.InvitationSystem.InvitationSystem.Dto.eventsDto.EventRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.eventsDto.EventResponseDto;
import com.InvitationSystem.InvitationSystem.entity.EventType;

import java.util.List;
import java.util.UUID;

public interface EventService {

    EventResponseDto createEvent(EventRequestDto request, UUID createdBy);

    EventResponseDto getEventById(UUID eventId);

    List<EventResponseDto> getEventsByCreator(UUID createdBy);

    List<EventResponseDto> getAllEvents();

    List<EventResponseDto> getEventsByType(EventType eventType);

    EventResponseDto updateEvent(UUID eventId, EventRequestDto request);

    EventResponseDto updateEventStatus(UUID eventId, String status);

    void deleteEvent(UUID eventId);
}