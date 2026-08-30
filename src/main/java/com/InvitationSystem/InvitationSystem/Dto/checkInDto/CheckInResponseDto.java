package com.InvitationSystem.InvitationSystem.Dto.checkInDto;

import com.InvitationSystem.InvitationSystem.entity.AdmissionType;
import com.InvitationSystem.InvitationSystem.entity.CheckInEntitlementState;
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
public class CheckInResponseDto {

    private UUID checkInId;
    private UUID invitationId;
    private UUID eventId;
    private String token;
    private CheckInResult result;
    private String message;
    private String guestName;
    private String eventName;
    private AdmissionType admissionType;
    private Integer admissionLimit;
    private Integer usedAdmissions;
    private Integer remainingAdmissions;
    private boolean revoked;
    private boolean belongsToScannedEvent;
    private CheckInEntitlementState entitlementState;
    private LocalDateTime scannedAt;
    private String scannerId;
}
