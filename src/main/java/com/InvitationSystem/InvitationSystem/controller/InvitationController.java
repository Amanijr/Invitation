package com.InvitationSystem.InvitationSystem.controller;

import com.InvitationSystem.InvitationSystem.Dto.checkInDto.CheckInRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.checkInDto.CheckInResponseDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationResponseDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationDetailedResponseDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationScanResponseDto;
import com.InvitationSystem.InvitationSystem.entity.InvitationStatus;
import com.InvitationSystem.InvitationSystem.entity.User;
import com.InvitationSystem.InvitationSystem.repository.UserRepository;
import com.InvitationSystem.InvitationSystem.security.DeskUsers;
import com.InvitationSystem.InvitationSystem.service.EventService;
import com.InvitationSystem.InvitationSystem.service.InvitationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.BatchRenderResultDto;
import com.InvitationSystem.InvitationSystem.service.ECardRenderingEngineService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invitations")
@Tag(name = "Invitations", description = "Invitation generation, tracking, and scanning endpoints")
public class InvitationController {

    @Autowired
    private InvitationService invitationService;

    @Autowired
    private ECardRenderingEngineService eCardRenderingEngineService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeskUsers deskUsers;

    @Autowired
    private EventService eventService;

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

    @GetMapping("/token/{token}/card")
    public ResponseEntity<byte[]> getInvitationCardByToken(@PathVariable String token) {
        try {
            byte[] imageBytes = eCardRenderingEngineService.renderCardImageBytesByToken(token);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"invitation-card.png\"")
                    .body(imageBytes);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
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
    public ResponseEntity<List<InvitationDetailedResponseDto>> getInvitationsByEvent(
            @PathVariable UUID eventId,
            Authentication authentication) {
        User user = deskUsers.require(authentication);
        eventService.assertCanAccess(eventId, user.getUserId(), user.getRole());
        List<InvitationDetailedResponseDto> invitations = invitationService.getInvitationsByEvent(eventId);
        return ResponseEntity.ok(invitations);
    }

    @GetMapping("/guest/{guestId}")
    public ResponseEntity<List<InvitationDetailedResponseDto>> getInvitationsByGuest(@PathVariable UUID guestId) {
        List<InvitationDetailedResponseDto> invitations = invitationService.getInvitationsByGuest(guestId);
        return ResponseEntity.ok(invitations);
    }

    @GetMapping
    public ResponseEntity<List<InvitationResponseDto>> getAllInvitations(Authentication authentication) {
        User user = deskUsers.require(authentication);
        List<InvitationResponseDto> invitations = invitationService.getInvitationsForActor(
                user.getUserId(), user.getRole());
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

    @PostMapping("/verify")
    public ResponseEntity<CheckInResponseDto> verifyInvitation(@jakarta.validation.Valid @RequestBody CheckInRequestDto request) {
        CheckInResponseDto response = invitationService.verifyInvitation(request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{invitationId}")
    public ResponseEntity<Void> deleteInvitation(@PathVariable UUID invitationId) {
        invitationService.deleteInvitation(invitationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{invitationId}/revoke")
    public ResponseEntity<InvitationDetailedResponseDto> revokeInvitation(
            @PathVariable UUID invitationId,
            Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Sign in to revoke invitations.");
        }
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found for " + authentication.getName()));
        InvitationDetailedResponseDto response = invitationService.revokeInvitation(
                invitationId, user.getUserId(), user.getRole());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/bulk-upload/{bulkUploadSessionId}")
    public ResponseEntity<List<InvitationDetailedResponseDto>> getInvitationsByBulkUpload(@PathVariable UUID bulkUploadSessionId) {
        List<InvitationDetailedResponseDto> invitations = invitationService.getInvitationsByBulkUpload(bulkUploadSessionId);
        return ResponseEntity.ok(invitations);
    }

    // ==========================================
    // PERSONALIZED E-CARD RENDERING ENDPOINTS
    // ==========================================

    @GetMapping("/{invitationId}/card")
    public ResponseEntity<byte[]> getInvitationCardFile(@PathVariable UUID invitationId) {
        byte[] imageBytes = eCardRenderingEngineService.renderCardImageBytes(invitationId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"ecard-" + invitationId + ".png\"")
                .body(imageBytes);
    }

    @PostMapping("/{invitationId}/render")
    public ResponseEntity<InvitationDetailedResponseDto> renderCard(@PathVariable UUID invitationId) {
        eCardRenderingEngineService.renderAndStoreCard(invitationId);
        InvitationDetailedResponseDto detailed = invitationService.getInvitationById(invitationId);
        return ResponseEntity.ok(detailed);
    }

    @PostMapping("/event/{eventId}/render-batch")
    public ResponseEntity<BatchRenderResultDto> renderBatchForEvent(@PathVariable UUID eventId) {
        BatchRenderResultDto result = eCardRenderingEngineService.renderBatchForEvent(eventId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/generate-bulk")
    public ResponseEntity<com.InvitationSystem.InvitationSystem.Dto.invitationsDto.BulkGenerationResultDto> generateBulkInvitations(
            @jakarta.validation.Valid @RequestBody com.InvitationSystem.InvitationSystem.Dto.invitationsDto.BulkGenerationRequestDto request) {
        com.InvitationSystem.InvitationSystem.Dto.invitationsDto.BulkGenerationResultDto result = invitationService.generateBulkInvitations(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
