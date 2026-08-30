package com.InvitationSystem.InvitationSystem.Dto.invitationsDto;

import com.InvitationSystem.InvitationSystem.entity.InvitationStatus;
import com.InvitationSystem.InvitationSystem.entity.DeliveryStatus;
import com.InvitationSystem.InvitationSystem.entity.RsvpStatus;
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
@io.swagger.v3.oas.annotations.media.Schema(description = "Detailed response payload for invitation lifecycle and tracking")
public class InvitationDetailedResponseDto {

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
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime openedAt;
    private RsvpStatus rsvpStatus;
    private LocalDateTime rsvpAt;
    private Integer partySize;
    private String dietaryNotes;
    private String mealChoice;
    private LocalDateTime generatedAt;
    private LocalDateTime expiryDate;
    private LocalDateTime expiresAt;
    private LocalDateTime scannedAt;
    private LocalDateTime usedAt;
    private UUID bulkUploadSessionId;
    private AdmissionType admissionType;
    private Integer admissionLimit;
    private Integer usedAdmissions;
    private Integer remainingAdmissions;
    private boolean revoked;
    private LocalDateTime revokedAt;
}
