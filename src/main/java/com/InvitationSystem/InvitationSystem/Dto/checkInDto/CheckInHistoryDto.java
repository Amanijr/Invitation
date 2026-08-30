package com.InvitationSystem.InvitationSystem.Dto.checkInDto;

import com.InvitationSystem.InvitationSystem.entity.CheckInResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckInHistoryDto {

    private UUID id;
    private UUID invitationId;
    private UUID eventId;
    private String scannerId;
    private String token;
    private CheckInResult result;
    private String notes;
    private LocalDateTime scannedAt;
}
