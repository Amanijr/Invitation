package com.InvitationSystem.InvitationSystem.controller;

import com.InvitationSystem.InvitationSystem.Dto.rsvpDto.RsvpGuestViewDto;
import com.InvitationSystem.InvitationSystem.Dto.rsvpDto.RsvpSubmitRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.rsvpDto.RsvpTrackingDto;
import com.InvitationSystem.InvitationSystem.service.RsvpService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rsvp")
@Tag(name = "RSVP", description = "Guest RSVP and host tracking")
public class RsvpController {

    @Autowired
    private RsvpService rsvpService;

    @GetMapping("/{token}")
    public ResponseEntity<RsvpGuestViewDto> openInvitation(@PathVariable String token) {
        return ResponseEntity.ok(rsvpService.openInvitation(token));
    }

    @PostMapping("/{token}")
    public ResponseEntity<RsvpGuestViewDto> submitRsvp(
            @PathVariable String token,
            @RequestBody RsvpSubmitRequestDto request) {
        return ResponseEntity.ok(rsvpService.submitRsvp(token, request));
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<RsvpTrackingDto> getTracking(@PathVariable UUID eventId) {
        return ResponseEntity.ok(rsvpService.getTracking(eventId));
    }
}
