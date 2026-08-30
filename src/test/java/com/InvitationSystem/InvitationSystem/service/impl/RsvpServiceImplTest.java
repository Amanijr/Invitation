package com.InvitationSystem.InvitationSystem.service.impl;

import com.InvitationSystem.InvitationSystem.Dto.rsvpDto.RsvpGuestViewDto;
import com.InvitationSystem.InvitationSystem.Dto.rsvpDto.RsvpSubmitRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.rsvpDto.RsvpTrackingDto;
import com.InvitationSystem.InvitationSystem.entity.Event;
import com.InvitationSystem.InvitationSystem.entity.Guest;
import com.InvitationSystem.InvitationSystem.entity.Invitation;
import com.InvitationSystem.InvitationSystem.entity.InvitationStatus;
import com.InvitationSystem.InvitationSystem.entity.RsvpStatus;
import com.InvitationSystem.InvitationSystem.repository.EventRepository;
import com.InvitationSystem.InvitationSystem.repository.GuestRepository;
import com.InvitationSystem.InvitationSystem.repository.InvitationRepository;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RsvpServiceImplTest {

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private GuestRepository guestRepository;

    @InjectMocks
    private RsvpServiceImpl rsvpService;

    private UUID eventId;
    private UUID guestId;
    private Invitation invitation;
    private Event event;
    private Guest guest;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        guestId = UUID.randomUUID();
        invitation = Invitation.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .guestId(guestId)
                .uniqueToken("abc123")
                .status(InvitationStatus.SENT)
                .rsvpStatus(RsvpStatus.NO_REPLY)
                .partySize(1)
                .expiresAt(LocalDateTime.now().plusDays(14))
                .build();
        event = Event.builder()
                .id(eventId)
                .eventName("Amani & Neema")
                .venue("The Slipway, Dar es Salaam")
                .eventDate(LocalDateTime.now().plusDays(30))
                .askDietary(true)
                .askMeal(true)
                .mealOptions("Chicken, Steak, Vegetarian pasta")
                .stayDetails("Serena Hotel — mention the sitting.")
                .build();
        guest = Guest.builder()
                .id(guestId)
                .eventId(eventId)
                .fullName("John Mwita")
                .build();
    }

    @Test
    void openInvitation_RecordsOpenedAtOnce() {
        when(invitationRepository.findByUniqueToken("abc123")).thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(guestRepository.findById(guestId)).thenReturn(Optional.of(guest));

        RsvpGuestViewDto first = rsvpService.openInvitation("abc123");
        assertEquals("John Mwita", first.getGuestName());
        assertEquals(RsvpStatus.NO_REPLY, first.getRsvpStatus());
        assertEquals(InvitationStatus.OPENED, invitation.getStatus());
        assertNotNull(invitation.getOpenedAt());

        LocalDateTime openedAt = invitation.getOpenedAt();
        rsvpService.openInvitation("abc123");
        assertEquals(openedAt, invitation.getOpenedAt());
        verify(invitationRepository).save(any(Invitation.class));
    }

    @Test
    void submitRsvp_SetsGoingAndPartySize() {
        invitation.setOpenedAt(LocalDateTime.now().minusHours(1));
        when(invitationRepository.findByUniqueToken("abc123")).thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(guestRepository.findById(guestId)).thenReturn(Optional.of(guest));

        RsvpGuestViewDto view = rsvpService.submitRsvp("abc123", RsvpSubmitRequestDto.builder()
                .status(RsvpStatus.GOING)
                .partySize(2)
                .mealChoice("Chicken")
                .dietaryNotes("No peanuts")
                .build());

        assertEquals(RsvpStatus.GOING, view.getRsvpStatus());
        assertEquals(2, view.getPartySize());
        assertEquals("Chicken", view.getMealChoice());
        assertEquals("No peanuts", view.getDietaryNotes());
        assertNotNull(invitation.getRsvpAt());
    }

    @Test
    void submitRsvp_RejectsNoReply() {
        assertThrows(IllegalArgumentException.class, () ->
                rsvpService.submitRsvp("abc123", RsvpSubmitRequestDto.builder()
                        .status(RsvpStatus.NO_REPLY)
                        .build()));
        verify(invitationRepository, never()).save(any());
    }

    @Test
    void openInvitation_ExpiredTokenFails() {
        invitation.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(invitationRepository.findByUniqueToken("abc123")).thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThrows(IllegalArgumentException.class, () -> rsvpService.openInvitation("abc123"));
        assertEquals(InvitationStatus.EXPIRED, invitation.getStatus());
    }

    @Test
    void getTracking_CountsAndActivity() {
        Invitation going = Invitation.builder()
                .eventId(eventId)
                .guestId(guestId)
                .uniqueToken("t1")
                .rsvpStatus(RsvpStatus.GOING)
                .openedAt(LocalDateTime.now().minusHours(2))
                .rsvpAt(LocalDateTime.now().minusHours(1))
                .partySize(2)
                .build();
        Invitation opened = Invitation.builder()
                .eventId(eventId)
                .uniqueToken("t2")
                .rsvpStatus(RsvpStatus.NO_REPLY)
                .openedAt(LocalDateTime.now().minusMinutes(10))
                .partySize(1)
                .build();

        when(eventRepository.existsById(eventId)).thenReturn(true);
        when(invitationRepository.findByEventId(eventId)).thenReturn(List.of(going, opened));
        when(guestRepository.findById(guestId)).thenReturn(Optional.of(guest));
        when(invitationRepository.countByEventIdAndOpenedAtIsNotNull(eventId)).thenReturn(2L);
        when(invitationRepository.countByEventIdAndRsvpStatus(eventId, RsvpStatus.GOING)).thenReturn(1L);
        when(invitationRepository.countByEventIdAndRsvpStatus(eventId, RsvpStatus.NOT_GOING)).thenReturn(0L);
        when(invitationRepository.countByEventIdAndRsvpStatus(eventId, RsvpStatus.MAYBE)).thenReturn(0L);
        when(invitationRepository.countByEventIdAndRsvpStatus(eventId, RsvpStatus.NO_REPLY)).thenReturn(1L);

        RsvpTrackingDto tracking = rsvpService.getTracking(eventId);
        assertEquals(2, tracking.getTotal());
        assertEquals(1, tracking.getGoing());
        assertEquals(1, tracking.getNoReply());
        assertEquals(2, tracking.getActivity().size());
        assertEquals("Guest opened the invitation", tracking.getActivity().get(0).getText());
        assertEquals("John Mwita is going", tracking.getActivity().get(1).getText());
    }
}
