package com.InvitationSystem.InvitationSystem.Dto.guestDto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuestImportSummaryDto {

    private UUID eventId;
    private int importedCount;
    private int skippedCount;
    private List<GuestResponseDto> importedGuests;
}
