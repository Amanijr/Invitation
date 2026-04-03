package com.InvitationSystem.InvitationSystem.service.impl;

import com.InvitationSystem.InvitationSystem.Dto.eventsDto.EventRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.eventsDto.EventResponseDto;
import com.InvitationSystem.InvitationSystem.entity.Event;
import com.InvitationSystem.InvitationSystem.entity.EventType;
import com.InvitationSystem.InvitationSystem.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventServiceImpl eventService;

    private UUID eventId;
    private UUID createdBy;
    private EventRequestDto request;
    private Event event;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        createdBy = UUID.randomUUID();

        request = new EventRequestDto(
                "Tech Summit",
                "Annual summit",
                "Main Hall",
                LocalDateTime.now().plusDays(3),
                EventType.CONFERENCE
        );

        event = Event.builder()
                .id(eventId)
                .eventName("Tech Summit")
                .eventDescription("Annual summit")
                .venue("Main Hall")
                .eventDate(request.getEventDate())
                .eventType(EventType.CONFERENCE)
                .status("ACTIVE")
                .createdBy(createdBy)
                .build();
    }

    @Test
    void createEvent_success() {
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        EventResponseDto response = eventService.createEvent(request, createdBy);

        assertEquals("Tech Summit", response.getEventName());
        assertEquals(EventType.CONFERENCE, response.getEventType());
    }

    @Test
    void getEventById_notFound_throws() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> eventService.getEventById(eventId));
    }

    @Test
    void getEventsByType_success() {
        when(eventRepository.findByEventTypeOrderByCreatedAtDesc(EventType.CONFERENCE)).thenReturn(List.of(event));

        List<EventResponseDto> result = eventService.getEventsByType(EventType.CONFERENCE);

        assertEquals(1, result.size());
        assertEquals("Tech Summit", result.get(0).getEventName());
    }
}
