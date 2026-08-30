package com.InvitationSystem.InvitationSystem.controller;

import com.InvitationSystem.InvitationSystem.Dto.eventsDto.EventGuestExperienceDto;
import com.InvitationSystem.InvitationSystem.Dto.eventsDto.EventRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.eventsDto.EventResponseDto;
import com.InvitationSystem.InvitationSystem.Dto.eventsDto.EventTemplateChangeRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.eventsDto.EventTemplateChangeResponseDto;
import com.InvitationSystem.InvitationSystem.entity.User;
import com.InvitationSystem.InvitationSystem.repository.UserRepository;
import com.InvitationSystem.InvitationSystem.service.EventService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Events", description = "Event management endpoints")
public class EventController {

    @Autowired
    private EventService eventService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<EventResponseDto> createEvent(
            @Valid @RequestBody EventRequestDto request,
            Authentication authentication) {
        EventResponseDto response = eventService.createEvent(request, requireUserId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponseDto> getEventById(
            @PathVariable UUID eventId,
            Authentication authentication) {
        User user = requireUser(authentication);
        eventService.assertCanAccess(eventId, user.getUserId(), user.getRole());
        EventResponseDto response = eventService.getEventById(eventId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/creator/{createdBy}")
    public ResponseEntity<List<EventResponseDto>> getEventsByCreator(
            @PathVariable UUID createdBy,
            Authentication authentication) {
        List<EventResponseDto> events = eventService.getEventsByCreator(requireUserId(authentication));
        return ResponseEntity.ok(events);
    }

    @GetMapping
    public ResponseEntity<List<EventResponseDto>> getAllEvents(Authentication authentication) {
        List<EventResponseDto> events = eventService.getEventsByCreator(requireUserId(authentication));
        return ResponseEntity.ok(events);
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<EventResponseDto> updateEvent(
            @PathVariable UUID eventId,
            @Valid @RequestBody EventRequestDto request,
            Authentication authentication) {
        User user = requireUser(authentication);
        eventService.assertCanAccess(eventId, user.getUserId(), user.getRole());
        EventResponseDto response = eventService.updateEvent(eventId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{eventId}/guest-experience")
    public ResponseEntity<EventResponseDto> updateGuestExperience(
            @PathVariable UUID eventId,
            @RequestBody EventGuestExperienceDto request,
            Authentication authentication) {
        User user = requireUser(authentication);
        eventService.assertCanAccess(eventId, user.getUserId(), user.getRole());
        return ResponseEntity.ok(eventService.updateGuestExperience(eventId, request));
    }

    @PatchMapping("/{eventId}/status")
    public ResponseEntity<EventResponseDto> updateEventStatus(
            @PathVariable UUID eventId,
            @RequestParam String status,
            Authentication authentication) {
        User user = requireUser(authentication);
        eventService.assertCanAccess(eventId, user.getUserId(), user.getRole());
        EventResponseDto response = eventService.updateEventStatus(eventId, status);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{eventId}/template")
    public ResponseEntity<EventTemplateChangeResponseDto> assignCurrentTemplate(
            @PathVariable UUID eventId,
            @Valid @RequestBody EventTemplateChangeRequestDto request,
            Authentication authentication) {
        User user = requireUser(authentication);
        EventTemplateChangeResponseDto response = eventService.assignCurrentTemplate(
                eventId, request, user.getUserId(), user.getRole());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID eventId, Authentication authentication) {
        User user = requireUser(authentication);
        eventService.assertCanAccess(eventId, user.getUserId(), user.getRole());
        eventService.deleteEvent(eventId);
        return ResponseEntity.noContent().build();
    }

    private UUID requireUserId(Authentication authentication) {
        return requireUser(authentication).getUserId();
    }

    private User requireUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Sign in to manage events.");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found for " + authentication.getName()));
    }
}
