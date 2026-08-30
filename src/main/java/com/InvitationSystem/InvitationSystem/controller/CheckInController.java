package com.InvitationSystem.InvitationSystem.controller;

import com.InvitationSystem.InvitationSystem.Dto.checkInDto.CheckInHistoryDto;
import com.InvitationSystem.InvitationSystem.Dto.checkInDto.CheckInRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.checkInDto.CheckInResponseDto;
import com.InvitationSystem.InvitationSystem.entity.User;
import com.InvitationSystem.InvitationSystem.security.DeskUsers;
import com.InvitationSystem.InvitationSystem.service.InvitationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/check-in")
@Tag(name = "CheckIn", description = "Secure one-time invitation verification endpoints")
public class CheckInController {

    @Autowired
    private InvitationService invitationService;

    @Autowired
    private DeskUsers deskUsers;

    @PostMapping("/verify")
    public ResponseEntity<CheckInResponseDto> verifyInvitation(@Valid @RequestBody CheckInRequestDto request) {
        CheckInResponseDto response = invitationService.verifyInvitation(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<CheckInHistoryDto>> getCheckInHistory(
            @RequestParam(required = false) UUID eventId,
            Authentication authentication) {
        User user = deskUsers.require(authentication);
        return ResponseEntity.ok(invitationService.getCheckInHistory(
                eventId, user.getUserId(), user.getRole()));
    }
}
