package com.InvitationSystem.InvitationSystem.Dto.invitationsDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@io.swagger.v3.oas.annotations.media.Schema(description = "Request payload for creating an invitation")
public class InvitationRequestDto {

    private UUID eventId;
    private UUID templateId;
    private UUID guestId;
    private String guestName;
    private String recipientPhone;
    private String recipientEmail;
    private LocalDateTime expiryDate;
    private Boolean attachPdf;
}
