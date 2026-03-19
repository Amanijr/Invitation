package com.InvitationSystem.InvitationSystem.Dto.eventsDto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventRequestDto {

    private String eventName;
    private String description;
    private String venue;
    private LocalDateTime eventDate;
}
