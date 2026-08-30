package com.InvitationSystem.InvitationSystem.Dto.checkInDto;

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
public class CheckInRequestDto {

    @NotBlank(message = "token is required")
    private String token;

    private String scannerId;

    @NotNull(message = "eventId is required")
    private UUID eventId;
}
