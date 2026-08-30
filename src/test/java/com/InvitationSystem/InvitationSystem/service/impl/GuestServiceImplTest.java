package com.InvitationSystem.InvitationSystem.service.impl;

import com.InvitationSystem.InvitationSystem.Dto.guestDto.GuestRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.guestDto.GuestResponseDto;
import com.InvitationSystem.InvitationSystem.entity.Event;
import com.InvitationSystem.InvitationSystem.entity.Guest;
import com.InvitationSystem.InvitationSystem.repository.EventRepository;
import com.InvitationSystem.InvitationSystem.repository.GuestRepository;
import com.InvitationSystem.InvitationSystem.repository.InvitationRepository;
import com.InvitationSystem.InvitationSystem.service.InvitationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuestServiceImplTest {

    @Mock
    private GuestRepository guestRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private InvitationService invitationService;

    @InjectMocks
    private GuestServiceImpl guestService;

    private UUID eventId;
    private GuestRequestDto requestDto;
    private Guest guest;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        requestDto = GuestRequestDto.builder()
                .eventId(eventId)
                .fullName("John Doe")
                .email("john@example.com")
                .phone("+1234567890")
                .build();

        guest = Guest.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .fullName("John Doe")
                .email("john@example.com")
                .phone("+1234567890")
                .build();
        lenient().when(invitationRepository.findByEventIdAndGuestId(any(), any())).thenReturn(Optional.empty());
    }

    @Test
    void createGuest_Success() {
        Event event = Event.builder().id(eventId).eventName("Amani & Neema Wedding").build();
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(guestRepository.existsByEventIdAndEmail(eventId, "john@example.com")).thenReturn(false);
        when(guestRepository.save(any(Guest.class))).thenReturn(guest);

        GuestResponseDto result = guestService.createGuest(requestDto);

        assertNotNull(result);
        assertEquals("John Doe", result.getFullName());
        assertEquals("john@example.com", result.getEmail());
        verify(guestRepository, times(1)).save(any(Guest.class));
    }

    @Test
    void createGuest_EventNotFound_ThrowsException() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> guestService.createGuest(requestDto));
        verify(guestRepository, never()).save(any());
    }

    @Test
    void createGuest_missingContact_throws() {
        requestDto.setEmail(null);
        requestDto.setPhone("  ");
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(
                Event.builder().id(eventId).eventName("Amani & Neema Wedding").build()));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> guestService.createGuest(requestDto));
        assertEquals("Enter a phone number or an email.", ex.getMessage());
        verify(guestRepository, never()).save(any());
    }

    @Test
    void findOrCreateGuest_ExistingEmail_ReturnsExisting() {
        when(guestRepository.findByEventIdAndEmail(eventId, "john@example.com"))
                .thenReturn(Optional.of(guest));

        GuestResponseDto result = guestService.findOrCreateGuest(eventId, "John Doe", "john@example.com", "+1234567890");

        assertNotNull(result);
        assertEquals(guest.getId(), result.getId());
        verify(guestRepository, never()).save(any());
    }

    @Test
    void getAllGuests_ReturnsMappedList() {
        when(guestRepository.findAll()).thenReturn(List.of(guest));

        var result = guestService.getAllGuests();

        assertEquals(1, result.size());
        assertEquals("John Doe", result.get(0).getFullName());
    }
}
