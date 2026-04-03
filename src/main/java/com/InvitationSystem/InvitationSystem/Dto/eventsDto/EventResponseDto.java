package com.InvitationSystem.InvitationSystem.Dto.eventsDto;

import com.InvitationSystem.InvitationSystem.entity.EventType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@io.swagger.v3.oas.annotations.media.Schema(description = "Response payload for event details")
public class EventResponseDto {

    private UUID id;
    private String eventName;
    private String eventDescription;
    private String venue;
    private LocalDateTime eventDate;
    private EventType eventType;
    private String status;
}
