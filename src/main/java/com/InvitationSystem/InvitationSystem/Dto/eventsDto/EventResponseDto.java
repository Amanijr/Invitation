package com.InvitationSystem.InvitationSystem.Dto.eventsDto;


import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class EventResponseDto {

    private UUID id;
    private String eventName;
    private String description;
    private String venue;
    private LocalDateTime eventDate;
    private String status;

}
