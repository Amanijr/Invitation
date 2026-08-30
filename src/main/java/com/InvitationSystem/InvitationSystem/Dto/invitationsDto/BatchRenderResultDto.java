package com.InvitationSystem.InvitationSystem.Dto.invitationsDto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchRenderResultDto {

    private UUID eventId;
    private int totalCount;
    private int successCount;
    private int failureCount;
    private List<String> errorLogs;
    private List<UUID> failedInvitationIds;
}
