package com.InvitationSystem.InvitationSystem.service.impl;

import com.InvitationSystem.InvitationSystem.Dto.checkInDto.CheckInRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.checkInDto.CheckInResponseDto;
import com.InvitationSystem.InvitationSystem.Dto.eventsDto.EventRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.eventsDto.EventResponseDto;
import com.InvitationSystem.InvitationSystem.Dto.eventsDto.EventTemplateChangeRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.guestDto.GuestRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.guestDto.GuestResponseDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.InvitationResponseDto;
import com.InvitationSystem.InvitationSystem.entity.*;
import com.InvitationSystem.InvitationSystem.repository.*;
import com.InvitationSystem.InvitationSystem.service.EventService;
import com.InvitationSystem.InvitationSystem.service.GuestService;
import com.InvitationSystem.InvitationSystem.service.InvitationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EventCentricInvitationPhaseXTest {

    @Autowired
    private EventService eventService;
    @Autowired
    private GuestService guestService;
    @Autowired
    private InvitationService invitationService;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private GuestRepository guestRepository;
    @Autowired
    private TemplateRepository templateRepository;
    @Autowired
    private InvitationRepository invitationRepository;
    @Autowired
    private CheckInRepository checkInRepository;
    @Autowired
    private UserRepository userRepository;

    private UUID adminId;
    private UUID managerId;

    @BeforeEach
    void setUp() {
        checkInRepository.deleteAllInBatch();
        invitationRepository.deleteAllInBatch();
        guestRepository.deleteAllInBatch();
        templateRepository.deleteAllInBatch();
        eventRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        adminId = userRepository.save(User.builder()
                .firstName("Asha")
                .lastName("Kileo")
                .email("asha.kileo@inviteflow.test")
                .passwordHash("hashed")
                .role(UserRole.ADMIN)
                .enabled(true)
                .build()).getUserId();
        managerId = userRepository.save(User.builder()
                .firstName("Neema")
                .lastName("Mwamba")
                .email("neema.mwamba@inviteflow.test")
                .passwordHash("hashed")
                .role(UserRole.EVENT_MANAGER)
                .enabled(true)
                .build()).getUserId();
    }

    @Test
    @DisplayName("Event current template is inherited by new guests and preserved after a later template change")
    void eventTemplateInheritanceAndHistory() {
        EventResponseDto event = createWedding(adminId);
        Template elegantGold = saveTemplate(event.getId(), "Elegant Gold");
        Template royalBurgundy = saveTemplate(event.getId(), "Royal Burgundy");

        eventService.assignCurrentTemplate(event.getId(), EventTemplateChangeRequestDto.builder()
                .templateId(elegantGold.getId())
                .scope(TemplateChangeScope.NEW_GUESTS_ONLY)
                .build(), adminId, UserRole.ADMIN);

        GuestResponseDto john = addGuest(event.getId(), "John Mwita", "john.mwita@example.co.tz", "+255712000001", AdmissionType.SINGLE);
        GuestResponseDto mary = addGuest(event.getId(), "Mary Joseph", "mary.joseph@example.co.tz", "+255712000002", AdmissionType.DOUBLE);

        Invitation johnInv = invitationRepository.findByEventIdAndGuestId(event.getId(), john.getId()).orElseThrow();
        Invitation maryInv = invitationRepository.findByEventIdAndGuestId(event.getId(), mary.getId()).orElseThrow();
        assertEquals(elegantGold.getId(), johnInv.getTemplateId());
        assertEquals(elegantGold.getId(), maryInv.getTemplateId());
        assertEquals(elegantGold.resolvedVersion(), johnInv.getTemplateVersion());
        assertEquals(AdmissionType.SINGLE, johnInv.getAdmissionType());
        assertEquals(1, johnInv.getAdmissionLimit());
        assertEquals(AdmissionType.DOUBLE, maryInv.getAdmissionType());
        assertEquals(2, maryInv.getAdmissionLimit());

        eventService.assignCurrentTemplate(event.getId(), EventTemplateChangeRequestDto.builder()
                .templateId(royalBurgundy.getId())
                .scope(TemplateChangeScope.NEW_GUESTS_ONLY)
                .build(), adminId, UserRole.ADMIN);

        GuestResponseDto peter = addGuest(event.getId(), "Peter Mushi", "peter.mushi@example.co.tz", "+255712000003", AdmissionType.SINGLE);
        Invitation peterInv = invitationRepository.findByEventIdAndGuestId(event.getId(), peter.getId()).orElseThrow();
        assertEquals(royalBurgundy.getId(), peterInv.getTemplateId());

        Invitation johnAfter = invitationRepository.findById(johnInv.getId()).orElseThrow();
        Invitation maryAfter = invitationRepository.findById(maryInv.getId()).orElseThrow();
        assertEquals(elegantGold.getId(), johnAfter.getTemplateId());
        assertEquals(elegantGold.getId(), maryAfter.getTemplateId());
        assertEquals(johnInv.getTemplateVersion(), johnAfter.getTemplateVersion());
    }

    @Test
    @DisplayName("SINGLE admits once; DOUBLE admits twice; a third scan is rejected")
    void singleAndDoubleAdmissionLimits() {
        EventResponseDto event = createWedding(adminId);
        Template template = saveTemplate(event.getId(), "Elegant Gold");
        eventService.assignCurrentTemplate(event.getId(), EventTemplateChangeRequestDto.builder()
                .templateId(template.getId())
                .scope(TemplateChangeScope.NEW_GUESTS_ONLY)
                .build(), adminId, UserRole.ADMIN);

        GuestResponseDto john = addGuest(event.getId(), "John Mwita", "john.mwita@example.co.tz", "+255712000011", AdmissionType.SINGLE);
        GuestResponseDto mary = addGuest(event.getId(), "Mary Joseph", "mary.joseph@example.co.tz", "+255712000012", AdmissionType.DOUBLE);
        Invitation single = invitationRepository.findByEventIdAndGuestId(event.getId(), john.getId()).orElseThrow();
        Invitation pair = invitationRepository.findByEventIdAndGuestId(event.getId(), mary.getId()).orElseThrow();

        CheckInResponseDto firstSingle = invitationService.verifyInvitation(scan(single.getUniqueToken(), event.getId()));
        assertEquals(CheckInResult.SUCCESS, firstSingle.getResult());
        assertEquals(1, firstSingle.getAdmissionLimit());
        assertEquals(0, firstSingle.getRemainingAdmissions());
        assertEquals(CheckInEntitlementState.FULLY_USED, firstSingle.getEntitlementState());

        CheckInResponseDto secondSingle = invitationService.verifyInvitation(scan(single.getUniqueToken(), event.getId()));
        assertEquals(CheckInResult.ALREADY_USED, secondSingle.getResult());

        CheckInResponseDto firstDouble = invitationService.verifyInvitation(scan(pair.getUniqueToken(), event.getId()));
        assertEquals(CheckInResult.SUCCESS, firstDouble.getResult());
        assertEquals(2, firstDouble.getAdmissionLimit());
        assertEquals(1, firstDouble.getRemainingAdmissions());
        assertEquals(CheckInEntitlementState.PARTIALLY_USED, firstDouble.getEntitlementState());
        assertEquals(pair.getUniqueToken(), firstDouble.getToken());

        CheckInResponseDto secondDouble = invitationService.verifyInvitation(scan(pair.getUniqueToken(), event.getId()));
        assertEquals(CheckInResult.SUCCESS, secondDouble.getResult());
        assertEquals(0, secondDouble.getRemainingAdmissions());
        assertEquals(CheckInEntitlementState.FULLY_USED, secondDouble.getEntitlementState());

        CheckInResponseDto thirdDouble = invitationService.verifyInvitation(scan(pair.getUniqueToken(), event.getId()));
        assertEquals(CheckInResult.ALREADY_USED, thirdDouble.getResult());
    }

    @Test
    @DisplayName("An invitation for one event is rejected at another event's door")
    void eventIsolation() {
        EventResponseDto wedding = createWedding(adminId);
        EventResponseDto birthday = eventService.createEvent(birthdayRequest(), adminId);
        Template template = saveTemplate(wedding.getId(), "Elegant Gold");
        eventService.assignCurrentTemplate(wedding.getId(), EventTemplateChangeRequestDto.builder()
                .templateId(template.getId())
                .scope(TemplateChangeScope.NEW_GUESTS_ONLY)
                .build(), adminId, UserRole.ADMIN);
        GuestResponseDto john = addGuest(wedding.getId(), "John Mwita", "john.mwita@example.co.tz", "+255712000021", AdmissionType.SINGLE);
        Invitation invitation = invitationRepository.findByEventIdAndGuestId(wedding.getId(), john.getId()).orElseThrow();

        CheckInResponseDto mismatch = invitationService.verifyInvitation(scan(invitation.getUniqueToken(), birthday.getId()));
        assertEquals(CheckInResult.EVENT_MISMATCH, mismatch.getResult());
        assertFalse(mismatch.isBelongsToScannedEvent());
        assertEquals("Invitation does not belong to this event", mismatch.getMessage());
    }

    @Test
    @DisplayName("Repeated guest-add and generate calls do not create a second active invitation")
    void duplicateGenerationProtected() {
        EventResponseDto event = createWedding(adminId);
        Template template = saveTemplate(event.getId(), "Elegant Gold");
        eventService.assignCurrentTemplate(event.getId(), EventTemplateChangeRequestDto.builder()
                .templateId(template.getId())
                .scope(TemplateChangeScope.NEW_GUESTS_ONLY)
                .build(), adminId, UserRole.ADMIN);
        GuestResponseDto john = addGuest(event.getId(), "John Mwita", "john.mwita@example.co.tz", "+255712000031", AdmissionType.SINGLE);

        assertThrows(IllegalArgumentException.class, () -> addGuest(event.getId(), "John Mwita", "john.mwita@example.co.tz", "+255712000031", AdmissionType.SINGLE));
        InvitationResponseDto again = invitationService.issueInheritedInvitation(event.getId(), john.getId(), AdmissionType.SINGLE);
        assertEquals(1, invitationRepository.findByEventId(event.getId()).size());
        assertEquals(john.getInvitationId(), again.getId());

        InvitationRequestDto createAgain = new InvitationRequestDto();
        createAgain.setEventId(event.getId());
        createAgain.setGuestId(john.getId());
        createAgain.setTemplateId(template.getId());
        createAgain.setRecipientEmail("john.mwita@example.co.tz");
        createAgain.setRecipientPhone("+255712000031");
        assertThrows(IllegalArgumentException.class, () -> invitationService.createInvitation(createAgain));
        assertEquals(1, invitationRepository.countByEventId(event.getId()));
    }

    @Test
    @DisplayName("GUEST role cannot change the event template or revoke invitations")
    void unauthorizedCannotMutateTemplateOrRevoke() {
        EventResponseDto event = createWedding(managerId);
        Template template = saveTemplate(event.getId(), "Elegant Gold");
        Template other = saveTemplate(event.getId(), "Royal Burgundy");
        eventService.assignCurrentTemplate(event.getId(), EventTemplateChangeRequestDto.builder()
                .templateId(template.getId())
                .scope(TemplateChangeScope.NEW_GUESTS_ONLY)
                .build(), managerId, UserRole.EVENT_MANAGER);
        GuestResponseDto john = addGuest(event.getId(), "John Mwita", "john.mwita@example.co.tz", "+255712000041", AdmissionType.SINGLE);
        Invitation invitation = invitationRepository.findByEventIdAndGuestId(event.getId(), john.getId()).orElseThrow();

        assertThrows(AccessDeniedException.class, () -> eventService.assignCurrentTemplate(event.getId(),
                EventTemplateChangeRequestDto.builder().templateId(other.getId()).scope(TemplateChangeScope.NEW_GUESTS_ONLY).build(),
                UUID.randomUUID(), UserRole.GUEST));
        assertThrows(AccessDeniedException.class, () -> invitationService.revokeInvitation(invitation.getId(), UUID.randomUUID(), UserRole.GUEST));

        Invitation stillActive = invitationRepository.findById(invitation.getId()).orElseThrow();
        assertFalse(stillActive.isRevoked());
        assertEquals(template.getId(), eventService.getEventById(event.getId()).getCurrentTemplateId());
        assertNotEquals(stillActive.getUniqueToken(), stillActive.getId().toString());
        assertFalse(stillActive.getQrCodeUrl().contains(stillActive.getId().toString()));
    }

    @Test
    @DisplayName("ALL_INVITATIONS regeneration requires explicit confirm; unsent scope leaves sent cards")
    void templateChangeScopes() {
        EventResponseDto event = createWedding(adminId);
        Template gold = saveTemplate(event.getId(), "Elegant Gold");
        Template burgundy = saveTemplate(event.getId(), "Royal Burgundy");
        eventService.assignCurrentTemplate(event.getId(), EventTemplateChangeRequestDto.builder()
                .templateId(gold.getId())
                .scope(TemplateChangeScope.NEW_GUESTS_ONLY)
                .build(), adminId, UserRole.ADMIN);
        GuestResponseDto john = addGuest(event.getId(), "John Mwita", "john.mwita@example.co.tz", "+255712000051", AdmissionType.SINGLE);
        GuestResponseDto mary = addGuest(event.getId(), "Mary Joseph", "mary.joseph@example.co.tz", "+255712000052", AdmissionType.DOUBLE);
        Invitation sent = invitationRepository.findByEventIdAndGuestId(event.getId(), john.getId()).orElseThrow();
        sent.setDeliveryStatus(DeliveryStatus.SENT_EMAIL);
        sent.setStatus(InvitationStatus.SENT);
        invitationRepository.save(sent);

        assertThrows(IllegalArgumentException.class, () -> eventService.assignCurrentTemplate(event.getId(),
                EventTemplateChangeRequestDto.builder()
                        .templateId(burgundy.getId())
                        .scope(TemplateChangeScope.ALL_INVITATIONS)
                        .confirm(false)
                        .build(), adminId, UserRole.ADMIN));

        eventService.assignCurrentTemplate(event.getId(), EventTemplateChangeRequestDto.builder()
                .templateId(burgundy.getId())
                .scope(TemplateChangeScope.UNSENT_INVITATIONS)
                .build(), adminId, UserRole.ADMIN);

        assertEquals(gold.getId(), invitationRepository.findById(sent.getId()).orElseThrow().getTemplateId());
        assertEquals(burgundy.getId(), invitationRepository.findByEventIdAndGuestId(event.getId(), mary.getId()).orElseThrow().getTemplateId());
    }

    @Test
    @DisplayName("A revoked invitation is rejected even when admissions remain")
    void revokedInvitationRejected() {
        EventResponseDto event = createWedding(adminId);
        Template template = saveTemplate(event.getId(), "Elegant Gold");
        eventService.assignCurrentTemplate(event.getId(), EventTemplateChangeRequestDto.builder()
                .templateId(template.getId())
                .scope(TemplateChangeScope.NEW_GUESTS_ONLY)
                .build(), adminId, UserRole.ADMIN);
        GuestResponseDto mary = addGuest(event.getId(), "Mary Joseph", "mary.joseph@example.co.tz", "+255712000061", AdmissionType.DOUBLE);
        Invitation invitation = invitationRepository.findByEventIdAndGuestId(event.getId(), mary.getId()).orElseThrow();
        invitationService.revokeInvitation(invitation.getId(), adminId, UserRole.ADMIN);

        CheckInResponseDto response = invitationService.verifyInvitation(scan(invitation.getUniqueToken(), event.getId()));
        assertEquals(CheckInResult.REVOKED, response.getResult());
        assertTrue(response.isRevoked());
        assertEquals(CheckInEntitlementState.REVOKED, response.getEntitlementState());
    }

    private EventResponseDto createWedding(UUID createdBy) {
        EventRequestDto request = new EventRequestDto();
        request.setEventName("Amani & Neema Wedding");
        request.setVenue("Hyatt Regency Dar es Salaam");
        request.setEventDate(LocalDateTime.now().plusDays(40).withHour(16).withMinute(0).withSecond(0).withNano(0));
        request.setEventType(EventType.WEDDING);
        request.setStatus("ACTIVE");
        return eventService.createEvent(request, createdBy);
    }

    private EventRequestDto birthdayRequest() {
        EventRequestDto request = new EventRequestDto();
        request.setEventName("Peter Birthday");
        request.setVenue("Mikocheni Garden");
        request.setEventDate(LocalDateTime.now().plusDays(20).withHour(18).withMinute(0).withSecond(0).withNano(0));
        request.setEventType(EventType.BIRTHDAY);
        request.setStatus("ACTIVE");
        return request;
    }

    private Template saveTemplate(UUID eventId, String name) {
        return templateRepository.save(Template.builder()
                .eventId(eventId)
                .eventType(EventType.WEDDING)
                .templateName(name)
                .content("<html>{{guestName}}</html>")
                .active(true)
                .version(1)
                .build());
    }

    private GuestResponseDto addGuest(UUID eventId, String name, String email, String phone, AdmissionType admissionType) {
        return guestService.createGuest(GuestRequestDto.builder()
                .eventId(eventId)
                .fullName(name)
                .email(email)
                .phone(phone)
                .admissionType(admissionType)
                .build());
    }

    private CheckInRequestDto scan(String token, UUID eventId) {
        return CheckInRequestDto.builder()
                .token(token)
                .eventId(eventId)
                .scannerId("Hyatt door")
                .build();
    }
}
