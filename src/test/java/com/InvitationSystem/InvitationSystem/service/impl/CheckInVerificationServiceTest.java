package com.InvitationSystem.InvitationSystem.service.impl;

import com.InvitationSystem.InvitationSystem.Dto.checkInDto.CheckInRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.checkInDto.CheckInResponseDto;
import com.InvitationSystem.InvitationSystem.entity.*;
import com.InvitationSystem.InvitationSystem.repository.*;
import com.InvitationSystem.InvitationSystem.service.InvitationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class CheckInVerificationServiceTest {

    @Autowired
    private InvitationService invitationService;

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private CheckInRepository checkInRepository;

    @Autowired
    private UserRepository userRepository;

    private UUID eventId;
    private UUID templateId;
    private UUID adminUserId;

    @BeforeEach
    void setUp() {
        checkInRepository.deleteAllInBatch();
        invitationRepository.deleteAllInBatch();
        guestRepository.deleteAllInBatch();
        templateRepository.deleteAllInBatch();
        eventRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        User admin = userRepository.save(User.builder()
                .firstName("Admin")
                .lastName("User")
                .email("admin@example.com")
                .passwordHash("hashed")
                .role(UserRole.ADMIN)
                .enabled(true)
                .build());
        adminUserId = admin.getUserId();

        Event event = eventRepository.save(Event.builder()
                .eventName("Annual Gala")
                .venue("Grand Ballroom")
                .eventDate(LocalDateTime.now().plusDays(10))
                .eventType(EventType.CONFERENCE)
                .createdBy(adminUserId)
                .status("ACTIVE")
                .build());
        eventId = event.getId();

        Template template = templateRepository.save(Template.builder()
                .templateName("Gala VIP Template")
                .eventId(eventId)
                .eventType(EventType.CONFERENCE)
                .content("Gala Content")
                .active(true)
                .build());
        templateId = template.getId();
    }

    private Invitation createTestInvitation(String name, String email, String phone, LocalDateTime expiresAt) {
        Guest guest = guestRepository.save(Guest.builder()
                .fullName(name)
                .email(email)
                .phone(phone)
                .eventId(eventId)
                .build());

        return invitationRepository.save(Invitation.builder()
                .eventId(eventId)
                .guestId(guest.getId())
                .templateId(templateId)
                .recipientEmail(email)
                .recipientPhone(phone)
                .uniqueToken("TOK-" + UUID.randomUUID())
                .status(InvitationStatus.SENT)
                .used(false)
                .scanned(false)
                .expiresAt(expiresAt)
                .build());
    }

    @Test
    @DisplayName("1. Valid first scan returns SUCCESS and marks invitation as USED")
    void testValidFirstScan_success() {
        Invitation inv = createTestInvitation("Jane Doe", "jane@example.com", "+1234567890", LocalDateTime.now().plusDays(5));

        CheckInRequestDto request = CheckInRequestDto.builder()
                .token(inv.getUniqueToken())
                .eventId(eventId)
                .scannerId("Main entrance")
                .build();

        CheckInResponseDto response = invitationService.verifyInvitation(request);

        assertNotNull(response);
        assertEquals(CheckInResult.SUCCESS, response.getResult());
        assertEquals("Check-in successful", response.getMessage());
        assertEquals("Jane Doe", response.getGuestName());
        assertEquals("Annual Gala", response.getEventName());

        Invitation updated = invitationRepository.findByUniqueToken(inv.getUniqueToken()).orElseThrow();
        assertTrue(updated.isUsed());
        assertTrue(updated.isScanned());
        assertEquals(InvitationStatus.USED, updated.getStatus());

        List<CheckIn> audits = checkInRepository.findByInvitationId(inv.getId());
        assertEquals(1, audits.size());
        assertEquals(CheckInResult.SUCCESS, audits.get(0).getResult());
    }

    @Test
    @DisplayName("2. Second scan of same QR returns ALREADY_USED and is REJECTED")
    void testSameQrSecondScan_rejected() {
        Invitation inv = createTestInvitation("John Smith", "john@example.com", "+1234567891", LocalDateTime.now().plusDays(5));

        CheckInRequestDto request = CheckInRequestDto.builder()
                .token(inv.getUniqueToken())
                .eventId(eventId)
                .scannerId("Main entrance")
                .build();

        // First scan
        CheckInResponseDto response1 = invitationService.verifyInvitation(request);
        assertEquals(CheckInResult.SUCCESS, response1.getResult());

        // Second scan
        CheckInResponseDto response2 = invitationService.verifyInvitation(request);
        assertEquals(CheckInResult.ALREADY_USED, response2.getResult());
        assertEquals("Already checked in", response2.getMessage());

        List<CheckIn> audits = checkInRepository.findByInvitationId(inv.getId());
        assertEquals(2, audits.size());
        assertTrue(audits.stream().anyMatch(c -> c.getResult() == CheckInResult.SUCCESS));
        assertTrue(audits.stream().anyMatch(c -> c.getResult() == CheckInResult.ALREADY_USED));
    }

    @Test
    @DisplayName("3. Invalid token returns INVALID_TOKEN without leaking guest PII")
    void testInvalidToken_rejected() {
        CheckInRequestDto request = CheckInRequestDto.builder()
                .token("NON-EXISTENT-TOKEN-999")
                .eventId(eventId)
                .scannerId("Main entrance")
                .build();

        CheckInResponseDto response = invitationService.verifyInvitation(request);

        assertEquals(CheckInResult.INVALID_TOKEN, response.getResult());
        assertNull(response.getGuestName(), "Do not leak sensitive guest information for invalid tokens");
        assertNull(response.getInvitationId());

        List<CheckIn> audits = checkInRepository.findAll();
        assertEquals(1, audits.size());
        assertEquals(CheckInResult.INVALID_TOKEN, audits.get(0).getResult());
    }

    @Test
    @DisplayName("4. Expired invitation returns EXPIRED and sets status to EXPIRED")
    void testExpiredInvitation_rejected() {
        Invitation inv = createTestInvitation("Expired User", "expired@example.com", "+1987654321", LocalDateTime.now().minusDays(1));

        CheckInRequestDto request = CheckInRequestDto.builder()
                .token(inv.getUniqueToken())
                .eventId(eventId)
                .scannerId("Main entrance")
                .build();

        CheckInResponseDto response = invitationService.verifyInvitation(request);

        assertEquals(CheckInResult.EXPIRED, response.getResult());
        assertEquals("Invitation has expired", response.getMessage());

        Invitation updated = invitationRepository.findByUniqueToken(inv.getUniqueToken()).orElseThrow();
        assertEquals(InvitationStatus.EXPIRED, updated.getStatus());
        assertFalse(updated.isUsed());
    }

    @Test
    @DisplayName("5. Wrong event ID returns EVENT_MISMATCH")
    void testWrongEvent_rejected() {
        Invitation inv = createTestInvitation("Wrong Event Guest", "wrong@example.com", "+1112223334", LocalDateTime.now().plusDays(5));
        UUID wrongEventId = UUID.randomUUID();

        CheckInRequestDto request = CheckInRequestDto.builder()
                .token(inv.getUniqueToken())
                .eventId(wrongEventId)
                .scannerId("Main entrance")
                .build();

        CheckInResponseDto response = invitationService.verifyInvitation(request);

        assertEquals(CheckInResult.EVENT_MISMATCH, response.getResult());
        assertEquals("Invitation does not belong to this event", response.getMessage());

        Invitation updated = invitationRepository.findByUniqueToken(inv.getUniqueToken()).orElseThrow();
        assertFalse(updated.isUsed());
    }

    @Test
    @DisplayName("5b. Missing eventId is rejected and does not consume an admission")
    void testMissingEventId_doesNotConsumeAdmission() {
        Invitation inv = createTestInvitation("Scope Guest", "scope@example.com", "+15550001111", LocalDateTime.now().plusDays(5));

        CheckInResponseDto response = invitationService.verifyInvitation(CheckInRequestDto.builder()
                .token(inv.getUniqueToken())
                .scannerId("Main entrance")
                .build());

        assertEquals(CheckInResult.EVENT_MISMATCH, response.getResult());
        assertEquals("eventId is required", response.getMessage());

        Invitation updated = invitationRepository.findByUniqueToken(inv.getUniqueToken()).orElseThrow();
        assertFalse(updated.isUsed());
        assertEquals(0, updated.resolvedUsedAdmissions());
    }

    @Test
    @DisplayName("6. Gate label is stored on the check-in audit and history includes the token")
    void testGateLabelStoredAndHistoryIncludesToken() {
        Invitation inv = createTestInvitation("Auth Test Guest", "auth@example.com", "+1555666777", LocalDateTime.now().plusDays(5));

        CheckInRequestDto request = CheckInRequestDto.builder()
                .token(inv.getUniqueToken())
                .eventId(eventId)
                .scannerId("Main entrance")
                .build();

        CheckInResponseDto response = invitationService.verifyInvitation(request);

        assertEquals(CheckInResult.SUCCESS, response.getResult());
        assertEquals("Main entrance", response.getScannerId());

        var history = invitationService.getCheckInHistory(eventId);
        assertTrue(history.stream().anyMatch(row ->
                inv.getUniqueToken().equals(row.getToken()) && "Main entrance".equals(row.getScannerId())));
    }

    @Test
    @DisplayName("7. Audit record creation for all verification attempts")
    void testAuditRecordCreation() {
        Invitation inv = createTestInvitation("Audit Guest", "audit@example.com", "+1999888777", LocalDateTime.now().plusDays(5));

        CheckInRequestDto request = CheckInRequestDto.builder()
                .token(inv.getUniqueToken())
                .eventId(eventId)
                .scannerId("Main entrance")
                .build();

        invitationService.verifyInvitation(request); // SUCCESS
        invitationService.verifyInvitation(request); // ALREADY_USED

        List<CheckIn> checkIns = checkInRepository.findByInvitationId(inv.getId());
        assertEquals(2, checkIns.size());
        assertNotNull(checkIns.get(0).getScannedAt());
        assertEquals("Main entrance", checkIns.get(0).getScannerId());
    }
}
