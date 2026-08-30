package com.InvitationSystem.InvitationSystem.Dto.invitationsDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkGenerationErrorDto {
    private UUID guestId;
    private String guestName;
    private String errorMessage;
}
