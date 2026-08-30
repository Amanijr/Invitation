package com.InvitationSystem.InvitationSystem.Dto.eventsDto;

import com.InvitationSystem.InvitationSystem.entity.TemplateChangeScope;
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
@io.swagger.v3.oas.annotations.media.Schema(description = "Assign or change the event's current invitation template")
public class EventTemplateChangeRequestDto {

    @NotNull(message = "templateId is required")
    private UUID templateId;

    @Builder.Default
    private TemplateChangeScope scope = TemplateChangeScope.NEW_GUESTS_ONLY;

    /**
     * Required true when scope is ALL_INVITATIONS.
     */
    private Boolean confirm;
}
