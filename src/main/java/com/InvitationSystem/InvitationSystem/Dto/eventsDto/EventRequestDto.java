package com.InvitationSystem.InvitationSystem.Dto.eventsDto;

import com.InvitationSystem.InvitationSystem.entity.EventType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@io.swagger.v3.oas.annotations.media.Schema(description = "Request payload for creating or updating an event")
public class EventRequestDto {

    private String eventName;
    private String eventDescription;
    private String venue;
    private LocalDateTime eventDate;

    @NotNull(message = "Event type cannot be null")
    private EventType eventType;
}
