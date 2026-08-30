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
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class OneTimeVerificationConcurrencyTest {

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
    private UUID guestId;
    private UUID templateId;
    private UUID adminUserId;
    private String token;

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
                .eventName("Gala Dinner")
                .venue("Grand Hall")
                .eventDate(LocalDateTime.now().plusDays(10))
                .eventType(EventType.CONFERENCE)
                .createdBy(adminUserId)
                .status("ACTIVE")
                .build());
        eventId = event.getId();

        Guest guest = guestRepository.save(Guest.builder()
                .fullName("John Doe")
                .email("john@example.com")
                .phone("+1234567890")
                .eventId(eventId)
                .build());
        guestId = guest.getId();

        Template template = templateRepository.save(Template.builder()
                .templateName("Gala Template")
                .eventId(eventId)
                .eventType(EventType.CONFERENCE)
                .content("Hello")
                .active(true)
                .build());
        templateId = template.getId();

        token = "CONCURRENCY-TOKEN-" + UUID.randomUUID();

        invitationRepository.save(Invitation.builder()
                .eventId(eventId)
                .guestId(guestId)
                .templateId(templateId)
                .recipientEmail("john@example.com")
                .recipientPhone("+1234567890")
                .uniqueToken(token)
                .status(InvitationStatus.SENT)
                .used(false)
                .scanned(false)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .build());
    }

    @Test
    @DisplayName("Mandatory Concurrency Test: Simultaneous verification requests allow EXACTLY ONE success")
    void testSimultaneousVerifications_onlyOneSucceeds() throws Exception {
        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numberOfThreads);

        List<Future<CheckInResponseDto>> futures = new ArrayList<>();

        for (int i = 0; i < numberOfThreads; i++) {
            futures.add(executorService.submit(() -> {
                startLatch.await(); // wait for simultaneous trigger
                try {
                    return invitationService.verifyInvitation(CheckInRequestDto.builder()
                            .token(token)
                            .eventId(eventId)
                            .scannerId("Main entrance")
                            .build());
                } finally {
                    finishLatch.countDown();
                }
            }));
        }

        // Release all threads simultaneously
        startLatch.countDown();
        finishLatch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger alreadyUsedCount = new AtomicInteger(0);

        for (Future<CheckInResponseDto> future : futures) {
            CheckInResponseDto dto = future.get();
            if (dto.getResult() == CheckInResult.SUCCESS) {
                successCount.incrementAndGet();
            } else if (dto.getResult() == CheckInResult.ALREADY_USED) {
                alreadyUsedCount.incrementAndGet();
            }
        }

        // QUALITY GATE VERIFICATION:
        // EXACTLY 1 SUCCESS, all remaining 9 REJECTED (ALREADY_USED)
        assertEquals(1, successCount.get(), "Exactly one verification request must succeed");
        assertEquals(numberOfThreads - 1, alreadyUsedCount.get(), "All other simultaneous verification requests must be rejected as ALREADY_USED");

        // DB Consistency Verification
        Invitation updatedInv = invitationRepository.findByUniqueToken(token).orElseThrow();
        assertTrue(updatedInv.isUsed());
        assertTrue(updatedInv.isScanned());
        assertEquals(InvitationStatus.USED, updatedInv.getStatus());
        assertNotNull(updatedInv.getScannedAt());
        assertNotNull(updatedInv.getUsedAt());

        // Audit Record Verification
        List<CheckIn> checkIns = checkInRepository.findByInvitationId(updatedInv.getId());
        assertEquals(numberOfThreads, checkIns.size());
        long dbSuccessCount = checkIns.stream().filter(c -> c.getResult() == CheckInResult.SUCCESS).count();
        long dbAlreadyUsedCount = checkIns.stream().filter(c -> c.getResult() == CheckInResult.ALREADY_USED).count();
        assertEquals(1, dbSuccessCount);
        assertEquals(numberOfThreads - 1, dbAlreadyUsedCount);
    }
}
