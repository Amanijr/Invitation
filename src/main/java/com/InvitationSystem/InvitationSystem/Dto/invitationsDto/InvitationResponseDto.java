package com.InvitationSystem.InvitationSystem.Dto.invitationsDto;

import com.InvitationSystem.InvitationSystem.entity.InvitationStatus;
import com.InvitationSystem.InvitationSystem.entity.DeliveryStatus;
import com.InvitationSystem.InvitationSystem.entity.AdmissionType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@io.swagger.v3.oas.annotations.media.Schema(description = "Summary response payload for invitation records")
public class InvitationResponseDto {

    private UUID id;
    private UUID eventId;
    private UUID templateId;
    private Integer templateVersion;
    private UUID guestId;
    private String recipientPhone;
    private String recipientEmail;
    private String guestName;
    private String uniqueToken;
    private String qrCodeUrl;
    private String qrCode;
    private String cardReference;
    private String cardUrl;
    @JsonProperty("used")
    private boolean isUsed;
    private boolean scanned;
    private InvitationStatus status;
    private DeliveryStatus deliveryStatus;
    private LocalDateTime generatedAt;
    private LocalDateTime expiryDate;
    private LocalDateTime expiresAt;
    private LocalDateTime scannedAt;
    private LocalDateTime usedAt;
    private AdmissionType admissionType;
    private Integer admissionLimit;
    private Integer usedAdmissions;
    private Integer remainingAdmissions;
    private boolean revoked;
}
