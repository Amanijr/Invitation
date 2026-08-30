package com.InvitationSystem.InvitationSystem.Dto.invitationsDto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkGenerationRequestDto {

    @NotNull(message = "eventId is required")
    private UUID eventId;

    private UUID templateId;

    private List<UUID> guestIds;

    @Builder.Default
    private RegenerationPolicy regenerationPolicy = RegenerationPolicy.SKIP_EXISTING;

    private LocalDateTime expiryDate;
}
