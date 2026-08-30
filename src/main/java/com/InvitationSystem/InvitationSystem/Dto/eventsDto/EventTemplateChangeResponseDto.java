package com.InvitationSystem.InvitationSystem.Dto.eventsDto;

import com.InvitationSystem.InvitationSystem.entity.TemplateChangeScope;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@io.swagger.v3.oas.annotations.media.Schema(description = "Result of changing an event's current template")
public class EventTemplateChangeResponseDto {

    private EventResponseDto event;
    private TemplateChangeScope scope;
    private int regeneratedCount;
    private int skippedCount;
    private String message;
}
