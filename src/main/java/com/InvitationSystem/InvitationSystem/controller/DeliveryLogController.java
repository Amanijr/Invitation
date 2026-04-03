package com.InvitationSystem.InvitationSystem.controller;

import com.InvitationSystem.InvitationSystem.Dto.deliveryLogsDto.DeliveryLogRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.deliveryLogsDto.DeliveryLogResponseDto;
import com.InvitationSystem.InvitationSystem.service.DeliveryLogService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/delivery-logs")
@CrossOrigin(origins = "*")
@Tag(name = "Delivery Logs", description = "Delivery attempt and status logging endpoints")
public class DeliveryLogController {

    @Autowired
    private DeliveryLogService deliveryLogService;

    @PostMapping
    public ResponseEntity<DeliveryLogResponseDto> createDeliveryLog(@RequestBody DeliveryLogRequestDto request) {
        DeliveryLogResponseDto response = deliveryLogService.createDeliveryLog(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{logId}")
    public ResponseEntity<DeliveryLogResponseDto> getDeliveryLogById(@PathVariable UUID logId) {
        DeliveryLogResponseDto response = deliveryLogService.getDeliveryLogById(logId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/invitation/{invitationId}")
    public ResponseEntity<List<DeliveryLogResponseDto>> getDeliveryLogsByInvitation(@PathVariable UUID invitationId) {
        List<DeliveryLogResponseDto> logs = deliveryLogService.getDeliveryLogsByInvitation(invitationId);
        return ResponseEntity.ok(logs);
    }

    @GetMapping
    public ResponseEntity<List<DeliveryLogResponseDto>> getAllDeliveryLogs() {
        List<DeliveryLogResponseDto> logs = deliveryLogService.getAllDeliveryLogs();
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<DeliveryLogResponseDto>> getLogsByStatus(@PathVariable String status) {
        List<DeliveryLogResponseDto> logs = deliveryLogService.getLogsByStatus(status);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/channel/{channel}")
    public ResponseEntity<List<DeliveryLogResponseDto>> getLogsByChannel(@PathVariable String channel) {
        List<DeliveryLogResponseDto> logs = deliveryLogService.getLogsByChannel(channel);
        return ResponseEntity.ok(logs);
    }

    @PatchMapping("/{logId}/mark-delivered")
    public ResponseEntity<DeliveryLogResponseDto> markAsDelivered(@PathVariable UUID logId, @RequestParam String providerResponse) {
        DeliveryLogResponseDto response = deliveryLogService.markAsDelivered(logId, providerResponse);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{logId}/mark-failed")
    public ResponseEntity<DeliveryLogResponseDto> markAsFailed(@PathVariable UUID logId, @RequestParam String providerResponse) {
        DeliveryLogResponseDto response = deliveryLogService.markAsFailed(logId, providerResponse);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{logId}")
    public ResponseEntity<Void> deleteDeliveryLog(@PathVariable UUID logId) {
        deliveryLogService.deleteDeliveryLog(logId);
        return ResponseEntity.noContent().build();
    }
}
