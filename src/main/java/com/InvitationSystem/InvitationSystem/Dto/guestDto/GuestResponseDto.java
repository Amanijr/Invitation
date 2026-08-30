package com.InvitationSystem.InvitationSystem.Dto.guestDto;

import com.InvitationSystem.InvitationSystem.entity.AdmissionType;
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
public class GuestResponseDto {

    private UUID id;
    private UUID eventId;
    private String fullName;
    private String phone;
    private String email;
    private UUID invitationId;
    private AdmissionType admissionType;
    private Integer admissionLimit;
    private UUID templateId;
    private Integer templateVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
