package com.InvitationSystem.InvitationSystem.controller;

import com.InvitationSystem.InvitationSystem.Dto.deliveryDto.*;
import com.InvitationSystem.InvitationSystem.Dto.deliveryLogsDto.DeliveryLogResponseDto;
import com.InvitationSystem.InvitationSystem.entity.User;
import com.InvitationSystem.InvitationSystem.security.DeskUsers;
import com.InvitationSystem.InvitationSystem.service.MultiChannelDeliveryService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/v1/deliveries")
@Tag(name = "Multi-Channel Delivery", description = "Endpoints for sending invitations via Email, SMS, and WhatsApp")
public class MultiChannelDeliveryController {

    @Autowired
    private MultiChannelDeliveryService multiChannelDeliveryService;

    @Autowired
    private DeskUsers deskUsers;

    @PostMapping("/send")
    @Operation(summary = "Send invitation across selected channels (Email, SMS, WhatsApp)")
    public ResponseEntity<MultiChannelDeliveryResponseDto> sendInvitation(@Valid @RequestBody DeliveryRequestDto request) {
        MultiChannelDeliveryResponseDto response = multiChannelDeliveryService.sendInvitation(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/batch-send")
    @Operation(summary = "Send invitations in batch across selected channels")
    public ResponseEntity<BatchDeliveryResponseDto> sendBatchInvitations(@Valid @RequestBody BatchDeliveryRequestDto request) {
        BatchDeliveryResponseDto response = multiChannelDeliveryService.sendBatchInvitations(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/logs/{logId}/retry")
    @Operation(summary = "Retry a failed delivery attempt")
    public ResponseEntity<DeliveryLogResponseDto> retryDelivery(@PathVariable UUID logId) {
        DeliveryLogResponseDto response = multiChannelDeliveryService.retryDelivery(logId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/logs")
    @Operation(summary = "Get delivery logs for the signed-in desk")
    public ResponseEntity<List<DeliveryLogResponseDto>> getAllLogs(Authentication authentication) {
        User user = deskUsers.require(authentication);
        List<DeliveryLogResponseDto> logs = multiChannelDeliveryService.getLogsForActor(
                user.getUserId(), user.getRole());
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/logs/invitation/{invitationId}")
    @Operation(summary = "Get delivery logs for a specific invitation")
    public ResponseEntity<List<DeliveryLogResponseDto>> getLogsByInvitation(@PathVariable UUID invitationId) {
        List<DeliveryLogResponseDto> logs = multiChannelDeliveryService.getLogsByInvitation(invitationId);
        return ResponseEntity.ok(logs);
    }
}
