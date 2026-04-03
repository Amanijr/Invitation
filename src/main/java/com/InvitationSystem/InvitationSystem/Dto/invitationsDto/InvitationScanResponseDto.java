package com.InvitationSystem.InvitationSystem.Dto.invitationsDto;

import com.InvitationSystem.InvitationSystem.entity.InvitationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@io.swagger.v3.oas.annotations.media.Schema(description = "Response payload returned when scanning an invitation QR token")
public class InvitationScanResponseDto {

    private UUID invitationId;
    private String token;
    private InvitationStatus status;
    private boolean scanned;
    private LocalDateTime scannedAt;
    private String message;
}
