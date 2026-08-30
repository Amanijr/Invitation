package com.InvitationSystem.InvitationSystem.service.impl;

import com.InvitationSystem.InvitationSystem.Dto.guestDto.GuestRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.guestDto.GuestResponseDto;
import com.InvitationSystem.InvitationSystem.entity.AdmissionType;
import com.InvitationSystem.InvitationSystem.entity.Event;
import com.InvitationSystem.InvitationSystem.entity.Guest;
import com.InvitationSystem.InvitationSystem.entity.Invitation;
import com.InvitationSystem.InvitationSystem.entity.UserRole;
import com.InvitationSystem.InvitationSystem.repository.EventRepository;
import com.InvitationSystem.InvitationSystem.repository.GuestRepository;
import com.InvitationSystem.InvitationSystem.repository.InvitationRepository;
import com.InvitationSystem.InvitationSystem.service.GuestService;
import com.InvitationSystem.InvitationSystem.service.InvitationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class GuestServiceImpl implements GuestService {

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private InvitationService invitationService;

    private static final java.util.regex.Pattern EMAIL_PATTERN = java.util.regex.Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final java.util.regex.Pattern PHONE_PATTERN = java.util.regex.Pattern.compile("^\\+?[0-9\\s\\-()]{7,22}$");

    @Override
    @Transactional
    public GuestResponseDto createGuest(GuestRequestDto request) {
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + request.getEventId()));

        if (request.getFullName() == null || request.getFullName().isBlank()) {
            throw new IllegalArgumentException("Full name is required");
        }

        String email = request.getEmail() != null ? request.getEmail().trim() : null;
        String phone = request.getPhone() != null ? request.getPhone().trim() : null;
        String fullName = request.getFullName().trim();

        if (email != null && !email.isBlank()) {
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                throw new IllegalArgumentException("Invalid email format");
            }
            if (guestRepository.existsByEventIdAndEmail(request.getEventId(), email)) {
                throw new IllegalArgumentException("Guest with email '" + email + "' already exists for this event");
            }
        } else {
            email = null;
        }

        if (phone != null && !phone.isBlank()) {
            if (!PHONE_PATTERN.matcher(phone).matches()) {
                throw new IllegalArgumentException("Invalid phone format");
            }
            if (guestRepository.existsByEventIdAndPhone(request.getEventId(), phone)) {
                throw new IllegalArgumentException("Guest with phone '" + phone + "' already exists for this event");
            }
        } else {
            phone = null;
        }
        if ((email == null || email.isBlank()) && (phone == null || phone.isBlank())) {
            throw new IllegalArgumentException("Enter a phone number or an email.");
        }

        if (guestRepository.existsByEventIdAndFullName(request.getEventId(), fullName)) {
            throw new IllegalArgumentException("Guest with name '" + fullName + "' already exists for this event");
        }

        Guest guest = Guest.builder()
                .eventId(request.getEventId())
                .fullName(fullName)
                .phone(phone)
                .email(email)
                .build();

        Guest savedGuest = guestRepository.save(guest);
        if (event.getCurrentTemplateId() != null) {
            invitationService.issueInheritedInvitation(
                    savedGuest.getEventId(),
                    savedGuest.getId(),
                    AdmissionType.fromNullable(request.getAdmissionType()));
        }
        return mapToDto(savedGuest);
    }

    @Override
    @Transactional(readOnly = true)
    public GuestResponseDto getGuestById(UUID guestId) {
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new IllegalArgumentException("Guest not found with ID: " + guestId));
        return mapToDto(guest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GuestResponseDto> getAllGuests() {
        return guestRepository.findAll().stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GuestResponseDto> getGuestsForActor(UUID actorId, UserRole role) {
        if (role == UserRole.ADMIN) {
            return getAllGuests();
        }
        List<UUID> eventIds = eventRepository.findByCreatedBy(actorId).stream()
                .map(Event::getId)
                .toList();
        if (eventIds.isEmpty()) {
            return List.of();
        }
        return guestRepository.findByEventIdIn(eventIds).stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GuestResponseDto> getGuestsByEvent(UUID eventId) {
        return guestRepository.findByEventId(eventId).stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional
    public GuestResponseDto updateGuest(UUID guestId, GuestRequestDto request) {
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new IllegalArgumentException("Guest not found with ID: " + guestId));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            guest.setFullName(request.getFullName().trim());
        }

        if (request.getEmail() != null) {
            String email = request.getEmail().trim();
            if (!email.isBlank()) {
                if (!EMAIL_PATTERN.matcher(email).matches()) {
                    throw new IllegalArgumentException("Invalid email format");
                }
                if (!email.equalsIgnoreCase(guest.getEmail()) && guestRepository.existsByEventIdAndEmail(guest.getEventId(), email)) {
                    throw new IllegalArgumentException("Guest with email '" + email + "' already exists for this event");
                }
                guest.setEmail(email);
            } else {
                guest.setEmail(null);
            }
        }

        if (request.getPhone() != null) {
            String phone = request.getPhone().trim();
            if (!phone.isBlank()) {
                if (!PHONE_PATTERN.matcher(phone).matches()) {
                    throw new IllegalArgumentException("Invalid phone format");
                }
                if (!phone.equals(guest.getPhone()) && guestRepository.existsByEventIdAndPhone(guest.getEventId(), phone)) {
                    throw new IllegalArgumentException("Guest with phone '" + phone + "' already exists for this event");
                }
                guest.setPhone(phone);
            } else {
                guest.setPhone(null);
            }
        }

        Guest updatedGuest = guestRepository.save(guest);
        return mapToDto(updatedGuest);
    }

    @Override
    @Transactional
    public void deleteGuest(UUID guestId) {
        if (!guestRepository.existsById(guestId)) {
            throw new IllegalArgumentException("Guest not found with ID: " + guestId);
        }
        guestRepository.deleteById(guestId);
    }

    @Override
    @Transactional
    public GuestResponseDto findOrCreateGuest(UUID eventId, String fullName, String email, String phone) {
        if (email != null && !email.isBlank()) {
            Optional<Guest> existing = guestRepository.findByEventIdAndEmail(eventId, email);
            if (existing.isPresent()) {
                return mapToDto(existing.get());
            }
        }

        if (phone != null && !phone.isBlank()) {
            Optional<Guest> existing = guestRepository.findByEventIdAndPhone(eventId, phone);
            if (existing.isPresent()) {
                return mapToDto(existing.get());
            }
        }

        String displayName = (fullName != null && !fullName.isBlank()) ? fullName : (email != null ? email : phone);
        if (displayName == null || displayName.isBlank()) {
            displayName = "Guest";
        }

        Guest newGuest = Guest.builder()
                .eventId(eventId)
                .fullName(displayName)
                .email(email)
                .phone(phone)
                .build();

        Guest saved = guestRepository.save(newGuest);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GuestResponseDto> searchGuests(UUID eventId, String query) {
        if (query == null || query.isBlank()) {
            return getGuestsByEvent(eventId);
        }
        return guestRepository.searchGuests(eventId, query).stream()
                .map(this::mapToDto)
                .toList();
    }

    private GuestResponseDto mapToDto(Guest guest) {
        Invitation invitation = invitationRepository.findByEventIdAndGuestId(guest.getEventId(), guest.getId())
                .orElse(null);
        GuestResponseDto.GuestResponseDtoBuilder builder = GuestResponseDto.builder()
                .id(guest.getId())
                .eventId(guest.getEventId())
                .fullName(guest.getFullName())
                .phone(guest.getPhone())
                .email(guest.getEmail())
                .createdAt(guest.getCreatedAt())
                .updatedAt(guest.getUpdatedAt());
        if (invitation != null) {
            builder.invitationId(invitation.getId())
                    .admissionType(AdmissionType.fromNullable(invitation.getAdmissionType()))
                    .admissionLimit(invitation.resolvedAdmissionLimit())
                    .templateId(invitation.getTemplateId())
                    .templateVersion(invitation.getTemplateVersion() == null ? 1 : invitation.getTemplateVersion());
        }
        return builder.build();
    }
}
