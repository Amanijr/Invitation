package com.InvitationSystem.InvitationSystem.service;

import com.InvitationSystem.InvitationSystem.Dto.eventsDto.EventGuestExperienceDto;
import com.InvitationSystem.InvitationSystem.Dto.eventsDto.EventRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.eventsDto.EventResponseDto;
import com.InvitationSystem.InvitationSystem.entity.EventType;

import java.util.List;
import java.util.UUID;

public interface EventService {

    EventResponseDto createEvent(EventRequestDto request, UUID createdBy);

    EventResponseDto getEventById(UUID eventId);

    void assertCanAccess(UUID eventId, UUID actorId, com.InvitationSystem.InvitationSystem.entity.UserRole actorRole);

    java.util.List<UUID> visibleEventIds(UUID actorId, com.InvitationSystem.InvitationSystem.entity.UserRole actorRole);

    List<EventResponseDto> getEventsByCreator(UUID createdBy);

    List<EventResponseDto> getAllEvents();

    List<EventResponseDto> getEventsByType(EventType eventType);

    EventResponseDto updateEvent(UUID eventId, EventRequestDto request);

    EventResponseDto updateGuestExperience(UUID eventId, EventGuestExperienceDto request);

    EventResponseDto updateEventStatus(UUID eventId, String status);

    com.InvitationSystem.InvitationSystem.Dto.eventsDto.EventTemplateChangeResponseDto assignCurrentTemplate(
            UUID eventId,
            com.InvitationSystem.InvitationSystem.Dto.eventsDto.EventTemplateChangeRequestDto request,
            UUID actorId,
            com.InvitationSystem.InvitationSystem.entity.UserRole actorRole);

    void deleteEvent(UUID eventId);
}