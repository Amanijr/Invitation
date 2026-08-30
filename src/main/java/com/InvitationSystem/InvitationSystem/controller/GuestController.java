package com.InvitationSystem.InvitationSystem.controller;

import com.InvitationSystem.InvitationSystem.Dto.guestDto.GuestImportConfirmRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.guestDto.GuestImportPreviewDto;
import com.InvitationSystem.InvitationSystem.Dto.guestDto.GuestImportSummaryDto;
import com.InvitationSystem.InvitationSystem.Dto.guestDto.GuestRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.guestDto.GuestResponseDto;
import com.InvitationSystem.InvitationSystem.entity.User;
import com.InvitationSystem.InvitationSystem.security.DeskUsers;
import com.InvitationSystem.InvitationSystem.service.EventService;
import com.InvitationSystem.InvitationSystem.service.GuestImportService;
import com.InvitationSystem.InvitationSystem.service.GuestService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/guests")
@Tag(name = "Guests", description = "Guest management endpoints")
public class GuestController {

    @Autowired
    private GuestService guestService;

    @Autowired
    private GuestImportService guestImportService;

    @Autowired
    private DeskUsers deskUsers;

    @Autowired
    private EventService eventService;

    @PostMapping
    public ResponseEntity<GuestResponseDto> createGuest(
            @Valid @RequestBody GuestRequestDto request,
            Authentication authentication) {
        requireEventAccess(request.getEventId(), authentication);
        GuestResponseDto response = guestService.createGuest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<GuestResponseDto>> getAllGuests(Authentication authentication) {
        User user = deskUsers.require(authentication);
        return ResponseEntity.ok(guestService.getGuestsForActor(user.getUserId(), user.getRole()));
    }

    @GetMapping("/{guestId}")
    public ResponseEntity<GuestResponseDto> getGuestById(
            @PathVariable UUID guestId,
            Authentication authentication) {
        GuestResponseDto response = requireGuestAccess(guestId, authentication);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<GuestResponseDto>> getGuestsByEvent(
            @PathVariable UUID eventId,
            Authentication authentication) {
        User user = deskUsers.require(authentication);
        eventService.assertCanAccess(eventId, user.getUserId(), user.getRole());
        List<GuestResponseDto> guests = guestService.getGuestsByEvent(eventId);
        return ResponseEntity.ok(guests);
    }

    @GetMapping("/search")
    public ResponseEntity<List<GuestResponseDto>> searchGuests(
            @RequestParam UUID eventId,
            @RequestParam(required = false, defaultValue = "") String query,
            Authentication authentication) {
        User user = deskUsers.require(authentication);
        eventService.assertCanAccess(eventId, user.getUserId(), user.getRole());
        List<GuestResponseDto> guests = guestService.searchGuests(eventId, query);
        return ResponseEntity.ok(guests);
    }

    @PutMapping("/{guestId}")
    public ResponseEntity<GuestResponseDto> updateGuest(
            @PathVariable UUID guestId,
            @Valid @RequestBody GuestRequestDto request,
            Authentication authentication) {
        GuestResponseDto existing = requireGuestAccess(guestId, authentication);
        if (!existing.getEventId().equals(request.getEventId())) {
            requireEventAccess(request.getEventId(), authentication);
        }
        GuestResponseDto response = guestService.updateGuest(guestId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{guestId}")
    public ResponseEntity<Void> deleteGuest(@PathVariable UUID guestId, Authentication authentication) {
        requireGuestAccess(guestId, authentication);
        guestService.deleteGuest(guestId);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // IMPORT WIZARD PIPELINE ENDPOINTS
    // ==========================================

    @PostMapping(value = "/import/preview", consumes = "multipart/form-data")
    public ResponseEntity<GuestImportPreviewDto> previewImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam("eventId") UUID eventId,
            @RequestParam(value = "deliveryChannel", required = false, defaultValue = "BOTH") String deliveryChannel,
            Authentication authentication) {
        requireEventAccess(eventId, authentication);
        GuestImportPreviewDto preview = guestImportService.previewImport(file, eventId, deliveryChannel);
        return ResponseEntity.ok(preview);
    }

    @PostMapping("/import/confirm")
    public ResponseEntity<GuestImportSummaryDto> confirmImport(
            @Valid @RequestBody GuestImportConfirmRequestDto request,
            Authentication authentication) {
        requireEventAccess(request.getEventId(), authentication);
        GuestImportSummaryDto summary = guestImportService.confirmImport(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(summary);
    }

    private void requireEventAccess(UUID eventId, Authentication authentication) {
        User user = deskUsers.require(authentication);
        eventService.assertCanAccess(eventId, user.getUserId(), user.getRole());
    }

    private GuestResponseDto requireGuestAccess(UUID guestId, Authentication authentication) {
        GuestResponseDto guest = guestService.getGuestById(guestId);
        requireEventAccess(guest.getEventId(), authentication);
        return guest;
    }
}
