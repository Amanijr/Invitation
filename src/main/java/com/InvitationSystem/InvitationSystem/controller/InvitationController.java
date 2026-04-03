package com.InvitationSystem.InvitationSystem.controller;

import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationResponseDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationDetailedResponseDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationScanResponseDto;
import com.InvitationSystem.InvitationSystem.entity.InvitationStatus;
import com.InvitationSystem.InvitationSystem.service.InvitationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invitations")
@CrossOrigin(origins = "*")
@Tag(name = "Invitations", description = "Invitation generation, tracking, and scanning endpoints")
public class InvitationController {

    @Autowired
    private InvitationService invitationService;

    @PostMapping
    public ResponseEntity<InvitationResponseDto> createInvitation(@RequestBody InvitationRequestDto request) {
        InvitationResponseDto response = invitationService.createInvitation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{invitationId}")
    public ResponseEntity<InvitationDetailedResponseDto> getInvitationById(@PathVariable UUID invitationId) {
        InvitationDetailedResponseDto response = invitationService.getInvitationById(invitationId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/token/{token}")
    public ResponseEntity<?> getInvitationByToken(@PathVariable String token,
                                                   @RequestParam(required = false) String recipientPhone,
                                                   @RequestParam(required = false) String recipientEmail) {
        try {
            invitationService.validateInvitation(token, recipientPhone, recipientEmail);
            InvitationDetailedResponseDto response = invitationService.getInvitationByToken(token);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<InvitationDetailedResponseDto>> getInvitationsByEvent(@PathVariable UUID eventId) {
        List<InvitationDetailedResponseDto> invitations = invitationService.getInvitationsByEvent(eventId);
        return ResponseEntity.ok(invitations);
    }

    @GetMapping("/guest/{guestId}")
    public ResponseEntity<List<InvitationDetailedResponseDto>> getInvitationsByGuest(@PathVariable UUID guestId) {
        List<InvitationDetailedResponseDto> invitations = invitationService.getInvitationsByGuest(guestId);
        return ResponseEntity.ok(invitations);
    }

    @GetMapping
    public ResponseEntity<List<InvitationResponseDto>> getAllInvitations() {
        List<InvitationResponseDto> invitations = invitationService.getAllInvitations();
        return ResponseEntity.ok(invitations);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<InvitationDetailedResponseDto>> getInvitationsByStatus(@PathVariable String status) {
        InvitationStatus invitationStatus = InvitationStatus.valueOf(status.toUpperCase());
        List<InvitationDetailedResponseDto> invitations = invitationService.getInvitationsByStatus(invitationStatus);
        return ResponseEntity.ok(invitations);
    }

    @PatchMapping("/{invitationId}/mark-sent")
    public ResponseEntity<InvitationDetailedResponseDto> markAsSent(@PathVariable UUID invitationId) {
        InvitationDetailedResponseDto response = invitationService.markAsSent(invitationId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{invitationId}/mark-delivered")
    public ResponseEntity<InvitationDetailedResponseDto> markAsDelivered(@PathVariable UUID invitationId) {
        InvitationDetailedResponseDto response = invitationService.markAsDelivered(invitationId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{invitationId}/mark-opened")
    public ResponseEntity<InvitationDetailedResponseDto> markAsOpened(@PathVariable UUID invitationId) {
        InvitationDetailedResponseDto response = invitationService.markAsOpened(invitationId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{invitationId}/mark-used")
    public ResponseEntity<InvitationDetailedResponseDto> markAsUsed(@PathVariable UUID invitationId) {
        InvitationDetailedResponseDto response = invitationService.markAsUsed(invitationId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{invitationId}/qrcode")
    public ResponseEntity<InvitationDetailedResponseDto> generateQrCode(@PathVariable UUID invitationId, @RequestParam String qrCodeUrl) {
        InvitationDetailedResponseDto response = invitationService.generateQrCode(invitationId, qrCodeUrl);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/scan/{token}")
    public ResponseEntity<InvitationScanResponseDto> scanInvitation(@PathVariable String token) {
        InvitationScanResponseDto response = invitationService.scanInvitationByToken(token);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{invitationId}")
    public ResponseEntity<Void> deleteInvitation(@PathVariable UUID invitationId) {
        invitationService.deleteInvitation(invitationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bulk-upload/{bulkUploadSessionId}")
    public ResponseEntity<List<InvitationDetailedResponseDto>> getInvitationsByBulkUpload(@PathVariable UUID bulkUploadSessionId) {
        List<InvitationDetailedResponseDto> invitations = invitationService.getInvitationsByBulkUpload(bulkUploadSessionId);
        return ResponseEntity.ok(invitations);
    }
}
