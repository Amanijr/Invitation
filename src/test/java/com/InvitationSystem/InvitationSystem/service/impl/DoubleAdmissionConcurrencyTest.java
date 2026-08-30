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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class DoubleAdmissionConcurrencyTest {

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
                .firstName("Asha")
                .lastName("Kileo")
                .email("asha.concurrency@inviteflow.test")
                .passwordHash("hashed")
                .role(UserRole.ADMIN)
                .enabled(true)
                .build());

        Event event = eventRepository.save(Event.builder()
                .eventName("Amani & Neema Wedding")
                .venue("Hyatt Regency Dar es Salaam")
                .eventDate(LocalDateTime.now().plusDays(10))
                .eventType(EventType.WEDDING)
                .createdBy(admin.getUserId())
                .status("ACTIVE")
                .build());
        eventId = event.getId();

        Guest guest = guestRepository.save(Guest.builder()
                .fullName("Mary Joseph")
                .email("mary.joseph@example.co.tz")
                .phone("+255712000099")
                .eventId(eventId)
                .build());

        Template template = templateRepository.save(Template.builder()
                .templateName("Elegant Gold")
                .eventId(eventId)
                .eventType(EventType.WEDDING)
                .content("Hello")
                .active(true)
                .version(1)
                .build());

        token = "DOUBLE-CONCURRENCY-" + UUID.randomUUID();
        invitationRepository.save(Invitation.builder()
                .eventId(eventId)
                .guestId(guest.getId())
                .templateId(template.getId())
                .templateVersion(1)
                .recipientEmail("mary.joseph@example.co.tz")
                .recipientPhone("+255712000099")
                .uniqueToken(token)
                .status(InvitationStatus.SENT)
                .used(false)
                .scanned(false)
                .admissionType(AdmissionType.DOUBLE)
                .admissionLimit(2)
                .usedAdmissions(0)
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .build());
    }

    @Test
    @DisplayName("Simultaneous DOUBLE scans cannot exceed admission limit 2")
    void simultaneousDoubleScans_exactlyTwoSucceed() throws Exception {
        int numberOfThreads = 8;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numberOfThreads);
        List<Future<CheckInResponseDto>> futures = new ArrayList<>();

        for (int i = 0; i < numberOfThreads; i++) {
            futures.add(executorService.submit(() -> {
                startLatch.await();
                try {
                    return invitationService.verifyInvitation(CheckInRequestDto.builder()
                            .token(token)
                            .eventId(eventId)
                            .scannerId("Hyatt door")
                            .build());
                } finally {
                    finishLatch.countDown();
                }
            }));
        }

        startLatch.countDown();
        finishLatch.await(15, TimeUnit.SECONDS);
        executorService.shutdown();

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger alreadyUsedCount = new AtomicInteger(0);
        for (Future<CheckInResponseDto> future : futures) {
            CheckInResult result = future.get().getResult();
            if (result == CheckInResult.SUCCESS) {
                successCount.incrementAndGet();
            } else if (result == CheckInResult.ALREADY_USED) {
                alreadyUsedCount.incrementAndGet();
            }
        }

        assertEquals(2, successCount.get());
        assertEquals(numberOfThreads - 2, alreadyUsedCount.get());

        Invitation updated = invitationRepository.findByUniqueToken(token).orElseThrow();
        assertEquals(2, updated.resolvedUsedAdmissions());
        assertEquals(0, updated.remainingAdmissions());
        assertEquals(InvitationStatus.USED, updated.getStatus());
    }
}
