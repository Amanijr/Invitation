package com.InvitationSystem.InvitationSystem.service;

import com.InvitationSystem.InvitationSystem.Dto.rsvpDto.RsvpGuestViewDto;
import com.InvitationSystem.InvitationSystem.Dto.rsvpDto.RsvpSubmitRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.rsvpDto.RsvpTrackingDto;

import java.util.UUID;

public interface RsvpService {
    RsvpGuestViewDto openInvitation(String token);

    RsvpGuestViewDto submitRsvp(String token, RsvpSubmitRequestDto request);

    RsvpTrackingDto getTracking(UUID eventId);
}
