package com.InvitationSystem.InvitationSystem.service;

import com.InvitationSystem.InvitationSystem.Dto.guestDto.GuestRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.guestDto.GuestResponseDto;
import com.InvitationSystem.InvitationSystem.entity.UserRole;

import java.util.List;
import java.util.UUID;

public interface GuestService {
    GuestResponseDto createGuest(GuestRequestDto request);
    GuestResponseDto getGuestById(UUID guestId);
    List<GuestResponseDto> getAllGuests();
    List<GuestResponseDto> getGuestsForActor(UUID actorId, UserRole role);
    List<GuestResponseDto> getGuestsByEvent(UUID eventId);
    GuestResponseDto updateGuest(UUID guestId, GuestRequestDto request);
    void deleteGuest(UUID guestId);
    GuestResponseDto findOrCreateGuest(UUID eventId, String fullName, String email, String phone);
    List<GuestResponseDto> searchGuests(UUID eventId, String query);
}
