package com.InvitationSystem.InvitationSystem.Dto.guestDto;

import com.InvitationSystem.InvitationSystem.entity.AdmissionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuestRequestDto {

    @NotNull(message = "eventId is required")
    private UUID eventId;

    @NotBlank(message = "fullName is required")
    private String fullName;

    private String phone;

    private String email;

    @Builder.Default
    private AdmissionType admissionType = AdmissionType.SINGLE;
}
