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
import com.InvitationSystem.InvitationSystem.service.RsvpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class RsvpServiceImpl implements RsvpService {

    private static final String DEFAULT_MEALS = "Chicken, Steak, Vegetarian pasta";

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private GuestRepository guestRepository;

    @Override
    @Transactional
    public RsvpGuestViewDto openInvitation(String token) {
        Invitation invitation = findActive(token);
        if (invitation.getOpenedAt() == null) {
            invitation.setOpenedAt(LocalDateTime.now());
            if (invitation.getStatus() == InvitationStatus.SENT
                    || invitation.getStatus() == InvitationStatus.DELIVERED
                    || invitation.getStatus() == InvitationStatus.GENERATED) {
                invitation.setStatus(InvitationStatus.OPENED);
            }
            invitation = invitationRepository.save(invitation);
        }
        return toGuestView(invitation);
    }

    @Override
    @Transactional
    public RsvpGuestViewDto submitRsvp(String token, RsvpSubmitRequestDto request) {
        if (request == null || request.getStatus() == null || request.getStatus() == RsvpStatus.NO_REPLY) {
            throw new IllegalArgumentException("RSVP status must be going, not going, or maybe");
        }
        Invitation invitation = findActive(token);
        if (invitation.getOpenedAt() == null) {
            invitation.setOpenedAt(LocalDateTime.now());
        }
        invitation.setRsvpStatus(request.getStatus());
        invitation.setRsvpAt(LocalDateTime.now());
        int size = request.getPartySize() == null ? 1 : request.getPartySize();
        if (size < 1 || size > 20) {
            throw new IllegalArgumentException("Party size must be between 1 and 20");
        }
        invitation.setPartySize(size);
        invitation.setDietaryNotes(blankToNull(request.getDietaryNotes()));
        invitation.setMealChoice(blankToNull(request.getMealChoice()));
        invitation = invitationRepository.save(invitation);
        return toGuestView(invitation);
    }

    @Override
    @Transactional(readOnly = true)
    public RsvpTrackingDto getTracking(UUID eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new IllegalArgumentException("Event not found with ID: " + eventId);
        }
        List<Invitation> invitations = invitationRepository.findByEventId(eventId);
        List<RsvpTrackingDto.RsvpGuestRowDto> rows = new ArrayList<>();
        List<RsvpTrackingDto.RsvpActivityDto> activity = new ArrayList<>();

        for (Invitation invitation : invitations) {
            String name = guestName(invitation);
            String last = lastActivity(invitation);
            rows.add(RsvpTrackingDto.RsvpGuestRowDto.builder()
                    .guestName(name)
                    .token(invitation.getUniqueToken())
                    .rsvpStatus(invitation.getRsvpStatus() == null ? RsvpStatus.NO_REPLY : invitation.getRsvpStatus())
                    .openedAt(invitation.getOpenedAt())
                    .rsvpAt(invitation.getRsvpAt())
                    .partySize(invitation.getPartySize())
                    .dietaryNotes(invitation.getDietaryNotes())
                    .mealChoice(invitation.getMealChoice())
                    .lastActivity(last)
                    .build());
            if (invitation.getRsvpAt() != null) {
                activity.add(RsvpTrackingDto.RsvpActivityDto.builder()
                        .guestName(name)
                        .text(activityText(name, invitation.getRsvpStatus()))
                        .status(invitation.getRsvpStatus().name())
                        .at(invitation.getRsvpAt())
                        .build());
            } else if (invitation.getOpenedAt() != null) {
                activity.add(RsvpTrackingDto.RsvpActivityDto.builder()
                        .guestName(name)
                        .text(name + " opened the invitation")
                        .status("OPENED")
                        .at(invitation.getOpenedAt())
                        .build());
            }
        }

        activity.sort(Comparator.comparing(RsvpTrackingDto.RsvpActivityDto::getAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        rows.sort(Comparator.comparing(RsvpTrackingDto.RsvpGuestRowDto::getGuestName, String.CASE_INSENSITIVE_ORDER));

        return RsvpTrackingDto.builder()
                .total(invitations.size())
                .opened(invitationRepository.countByEventIdAndOpenedAtIsNotNull(eventId))
                .going(invitationRepository.countByEventIdAndRsvpStatus(eventId, RsvpStatus.GOING))
                .notGoing(invitationRepository.countByEventIdAndRsvpStatus(eventId, RsvpStatus.NOT_GOING))
                .maybe(invitationRepository.countByEventIdAndRsvpStatus(eventId, RsvpStatus.MAYBE))
                .noReply(invitationRepository.countByEventIdAndRsvpStatus(eventId, RsvpStatus.NO_REPLY))
                .guests(rows)
                .activity(activity)
                .build();
    }

    private Invitation findActive(String token) {
        Invitation invitation = invitationRepository.findByUniqueToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid invitation token"));
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = invitation.getExpiresAt() != null ? invitation.getExpiresAt() : invitation.getExpiryDate();
        if (expiresAt != null && now.isAfter(expiresAt)) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new IllegalArgumentException("Invitation expired");
        }
        return invitation;
    }

    private RsvpGuestViewDto toGuestView(Invitation invitation) {
        Event event = eventRepository.findById(invitation.getEventId())
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        String meals = event.getMealOptions() == null || event.getMealOptions().isBlank()
                ? DEFAULT_MEALS
                : event.getMealOptions();
        List<String> options = Arrays.stream(meals.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        return RsvpGuestViewDto.builder()
                .token(invitation.getUniqueToken())
                .guestName(guestName(invitation))
                .eventName(event.getEventName())
                .venue(event.getVenue())
                .eventDate(event.getEventDate())
                .stayDetails(event.getStayDetails())
                .registryUrl(event.getRegistryUrl())
                .registryLabel(event.getRegistryLabel())
                .askDietary(event.getAskDietary() == null || event.getAskDietary())
                .askMeal(event.getAskMeal() == null || event.getAskMeal())
                .mealOptions(options)
                .rsvpStatus(invitation.getRsvpStatus() == null ? RsvpStatus.NO_REPLY : invitation.getRsvpStatus())
                .partySize(invitation.getPartySize() == null ? 1 : invitation.getPartySize())
                .dietaryNotes(invitation.getDietaryNotes())
                .mealChoice(invitation.getMealChoice())
                .checkedIn(invitation.isScanned() || invitation.isUsed())
                .build();
    }

    private String guestName(Invitation invitation) {
        if (invitation.getGuestId() == null) {
            return "Guest";
        }
        return guestRepository.findById(invitation.getGuestId())
                .map(Guest::getFullName)
                .orElse("Guest");
    }

    private String lastActivity(Invitation invitation) {
        if (invitation.getRsvpAt() != null) {
            return activityText(guestName(invitation), invitation.getRsvpStatus());
        }
        if (invitation.getOpenedAt() != null) {
            return "Opened";
        }
        return "No reply";
    }

    private String activityText(String name, RsvpStatus status) {
        if (status == RsvpStatus.GOING) {
            return name + " is going";
        }
        if (status == RsvpStatus.NOT_GOING) {
            return name + " can't make it";
        }
        if (status == RsvpStatus.MAYBE) {
            return name + " replied maybe";
        }
        return name + " replied";
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
