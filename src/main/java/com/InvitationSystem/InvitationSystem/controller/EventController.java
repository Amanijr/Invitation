package com.InvitationSystem.InvitationSystem.controller;

import com.InvitationSystem.InvitationSystem.Dto.eventsDto.EventRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.eventsDto.EventResponseDto;
import com.InvitationSystem.InvitationSystem.service.EventService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@CrossOrigin(origins = "*")
@Tag(name = "Events", description = "Event management endpoints")
public class EventController {

    @Autowired
    private EventService eventService;

    @PostMapping
    public ResponseEntity<EventResponseDto> createEvent(@Valid @RequestBody EventRequestDto request, @RequestParam UUID createdBy) {
        EventResponseDto response = eventService.createEvent(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponseDto> getEventById(@PathVariable UUID eventId) {
        EventResponseDto response = eventService.getEventById(eventId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/creator/{createdBy}")
    public ResponseEntity<List<EventResponseDto>> getEventsByCreator(@PathVariable UUID createdBy) {
        List<EventResponseDto> events = eventService.getEventsByCreator(createdBy);
        return ResponseEntity.ok(events);
    }

    @GetMapping
    public ResponseEntity<List<EventResponseDto>> getAllEvents() {
        List<EventResponseDto> events = eventService.getAllEvents();
        return ResponseEntity.ok(events);
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<EventResponseDto> updateEvent(@PathVariable UUID eventId, @Valid @RequestBody EventRequestDto request) {
        EventResponseDto response = eventService.updateEvent(eventId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{eventId}/status")
    public ResponseEntity<EventResponseDto> updateEventStatus(@PathVariable UUID eventId, @RequestParam String status) {
        EventResponseDto response = eventService.updateEventStatus(eventId, status);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID eventId) {
        eventService.deleteEvent(eventId);
        return ResponseEntity.noContent().build();
    }
}
