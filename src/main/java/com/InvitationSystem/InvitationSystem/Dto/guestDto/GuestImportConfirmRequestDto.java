package com.InvitationSystem.InvitationSystem.Dto.guestDto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuestImportConfirmRequestDto {

    @NotNull(message = "Event ID is required")
    private UUID eventId;

    private String deliveryChannel;

    private List<GuestImportRowDto> rowsToImport;
}
