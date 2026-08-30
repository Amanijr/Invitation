package com.InvitationSystem.InvitationSystem.service.impl;

import com.InvitationSystem.InvitationSystem.Dto.checkInDto.CheckInRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.checkInDto.CheckInResponseDto;

import com.InvitationSystem.InvitationSystem.entity.*;
import com.InvitationSystem.InvitationSystem.repository.*;
import com.InvitationSystem.InvitationSystem.service.InvitationService;
import com.InvitationSystem.InvitationSystem.service.MultiChannelDeliveryService;
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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class EndToEndMvpAcceptanceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private TemplateFieldConfigRepository fieldConfigRepository;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private DeliveryLogRepository deliveryLogRepository;

    @Autowired
    private CheckInRepository checkInRepository;

    @Autowired
    private InvitationService invitationService;

    @Autowired
    private MultiChannelDeliveryService multiChannelDeliveryService;

    private User adminUser;
    private Event mvpEvent;
    private Template mvpTemplate;

    private Guest guestA;
    private Guest guestB;
    private Guest guestC;

    private Invitation invA;
    private Invitation invB;
    private Invitation invC;

    @BeforeEach
    void setUp() {
        checkInRepository.deleteAllInBatch();
        deliveryLogRepository.deleteAllInBatch();
        invitationRepository.deleteAllInBatch();
        guestRepository.deleteAllInBatch();
        fieldConfigRepository.deleteAllInBatch();
        templateRepository.deleteAllInBatch();
        eventRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        // 1. Admin login / account setup
        adminUser = userRepository.save(User.builder()
                .firstName("Super")
                .lastName("Admin")
                .email("admin.mvp@example.com")
                .passwordHash("hashed_secret")
                .role(UserRole.ADMIN)
                .enabled(true)
                .build());

        // 2. Admin creates an event: "MVP Test Event"
        mvpEvent = eventRepository.save(Event.builder()
                .eventName("MVP Test Event")
                .venue("Metropolitan Convention Center")
                .eventDate(LocalDateTime.now().plusDays(14))
                .eventType(EventType.CORPORATE)
                .createdBy(adminUser.getUserId())
                .status("ACTIVE")
                .build());

        // 3. Admin uploads an invitation template
        mvpTemplate = templateRepository.save(Template.builder()
                .templateName("VIP Modern Card")
                .eventId(mvpEvent.getId())
                .eventType(EventType.CORPORATE)
                .content("<div class='card'><h1>{{guestName}}</h1><p>{{eventName}}</p><img src='{{qrCodeUrl}}'/></div>")
                .active(true)
                .build());

        // 4. Admin configures personalized fields
        fieldConfigRepository.save(TemplateFieldConfig.builder()
                .templateId(mvpTemplate.getId())
                .fieldType(FieldType.GUEST_NAME)
                .x(10.0)
                .y(20.0)
                .width(40.0)
                .height(10.0)
                .sampleText("Sample Guest Name")
                .build());

        fieldConfigRepository.save(TemplateFieldConfig.builder()
                .templateId(mvpTemplate.getId())
                .fieldType(FieldType.QR_CODE)
                .x(60.0)
                .y(60.0)
                .width(30.0)
                .height(30.0)
                .build());

        // 5. Admin imports/adds guests: Guest A, Guest B, Guest C
        guestA = guestRepository.save(Guest.builder()
                .fullName("Guest A")
                .email("guestA@example.com")
                .phone("+1111111111")
                .eventId(mvpEvent.getId())
                .build());

        guestB = guestRepository.save(Guest.builder()
                .fullName("Guest B")
                .email("guestB@example.com")
                .phone("+2222222222")
                .eventId(mvpEvent.getId())
                .build());

        guestC = guestRepository.save(Guest.builder()
                .fullName("Guest C")
                .email("guestC@example.com")
                .phone("+3333333333")
                .eventId(mvpEvent.getId())
                .build());
    }

    @Test
    @DisplayName("Complete 19-Step MVP Acceptance Test Workflow")
    void testCompleteMvpWorkflow() {
        // 6. Admin verifies guest data
        assertEquals(3, guestRepository.countByEventId(mvpEvent.getId()));

        // 7-9. Admin generates invitations for Guest A, B, C with personalized cards & unique QRs
        invA = invitationRepository.save(Invitation.builder()
                .eventId(mvpEvent.getId())
                .guestId(guestA.getId())
                .templateId(mvpTemplate.getId())
                .recipientEmail(guestA.getEmail())
                .recipientPhone(guestA.getPhone())
                .uniqueToken("TOK-MVP-A-" + UUID.randomUUID())
                .qrCode("data:image/png;base64,QR_A_CONTENT")
                .status(InvitationStatus.GENERATED)
                .used(false)
                .scanned(false)
                .expiresAt(LocalDateTime.now().plusDays(10))
                .build());

        invB = invitationRepository.save(Invitation.builder()
                .eventId(mvpEvent.getId())
                .guestId(guestB.getId())
                .templateId(mvpTemplate.getId())
                .recipientEmail(guestB.getEmail())
                .recipientPhone(guestB.getPhone())
                .uniqueToken("TOK-MVP-B-" + UUID.randomUUID())
                .qrCode("data:image/png;base64,QR_B_CONTENT")
                .status(InvitationStatus.GENERATED)
                .used(false)
                .scanned(false)
                .expiresAt(LocalDateTime.now().plusDays(10))
                .build());

        invC = invitationRepository.save(Invitation.builder()
                .eventId(mvpEvent.getId())
                .guestId(guestC.getId())
                .templateId(mvpTemplate.getId())
                .recipientEmail(guestC.getEmail())
                .recipientPhone(guestC.getPhone())
                .uniqueToken("TOK-MVP-C-" + UUID.randomUUID())
                .qrCode("data:image/png;base64,QR_C_CONTENT")
                .status(InvitationStatus.GENERATED)
                .used(false)
                .scanned(false)
                .expiresAt(LocalDateTime.now().plusDays(10))
                .build());

        // Real-world scenario assertions: Token & QR Uniqueness
        assertNotEquals(invA.getUniqueToken(), invB.getUniqueToken());
        assertNotEquals(invB.getUniqueToken(), invC.getUniqueToken());
        assertNotEquals(invA.getUniqueToken(), invC.getUniqueToken());

        assertNotEquals(invA.getQrCode(), invB.getQrCode());
        assertNotEquals(invB.getQrCode(), invC.getQrCode());
        assertNotEquals(invA.getQrCode(), invC.getQrCode());

        // 10-12. Delivery dispatches & logs
        deliveryLogRepository.save(DeliveryLog.builder()
                .invitationId(invA.getId())
                .guestId(guestA.getId())
                .channel("EMAIL")
                .status("DELIVERED")
                .recipientContact(guestA.getEmail())
                .build());

        deliveryLogRepository.save(DeliveryLog.builder()
                .invitationId(invB.getId())
                .guestId(guestB.getId())
                .channel("SMS")
                .status("DELIVERED")
                .recipientContact(guestB.getPhone())
                .build());

        deliveryLogRepository.save(DeliveryLog.builder()
                .invitationId(invC.getId())
                .guestId(guestC.getId())
                .channel("WHATSAPP")
                .status("DELIVERED")
                .recipientContact(guestC.getPhone())
                .build());

        assertEquals(3, deliveryLogRepository.count());

        // 13-17. Guest A: 1st Scan -> SUCCESS, status USED
        CheckInRequestDto scanReqA1 = CheckInRequestDto.builder()
                .token(invA.getUniqueToken())
                .eventId(mvpEvent.getId())
                .scannerId("Main entrance")
                .build();

        CheckInResponseDto resA1 = invitationService.verifyInvitation(scanReqA1);
        assertEquals(CheckInResult.SUCCESS, resA1.getResult());
        assertEquals("Guest A", resA1.getGuestName());
        assertEquals("MVP Test Event", resA1.getEventName());

        Invitation updatedA = invitationRepository.findById(invA.getId()).orElseThrow();
        assertTrue(updatedA.isUsed());
        assertEquals(InvitationStatus.USED, updatedA.getStatus());

        // 18-19. Guest A: 2nd Scan -> REJECTED (ALREADY_USED)
        CheckInResponseDto resA2 = invitationService.verifyInvitation(scanReqA1);
        assertEquals(CheckInResult.ALREADY_USED, resA2.getResult());

        // Guest B: 1st Scan -> SUCCESS
        CheckInRequestDto scanReqB = CheckInRequestDto.builder()
                .token(invB.getUniqueToken())
                .eventId(mvpEvent.getId())
                .scannerId("Main entrance")
                .build();
        CheckInResponseDto resB = invitationService.verifyInvitation(scanReqB);
        assertEquals(CheckInResult.SUCCESS, resB.getResult());

        // Guest C: 1st Scan -> SUCCESS
        CheckInRequestDto scanReqC = CheckInRequestDto.builder()
                .token(invC.getUniqueToken())
                .eventId(mvpEvent.getId())
                .scannerId("Main entrance")
                .build();
        CheckInResponseDto resC = invitationService.verifyInvitation(scanReqC);
        assertEquals(CheckInResult.SUCCESS, resC.getResult());

        // Invalid Token test -> REJECTED (INVALID_TOKEN)
        CheckInRequestDto scanInvalid = CheckInRequestDto.builder()
                .token("INVALID-NON-EXISTENT-TOKEN-999")
                .eventId(mvpEvent.getId())
                .scannerId("Main entrance")
                .build();
        CheckInResponseDto resInvalid = invitationService.verifyInvitation(scanInvalid);
        assertEquals(CheckInResult.INVALID_TOKEN, resInvalid.getResult());
    }

    @Test
    @DisplayName("Multithreaded Scanner Concurrency Test: 2 simultaneous scans -> Exactly 1 Success, 1 Rejection")
    void testMultithreadedScannerConcurrency() throws Exception {
        Invitation concInv = invitationRepository.save(Invitation.builder()
                .eventId(mvpEvent.getId())
                .guestId(guestA.getId())
                .templateId(mvpTemplate.getId())
                .recipientEmail(guestA.getEmail())
                .uniqueToken("TOK-CONC-TEST-" + UUID.randomUUID())
                .status(InvitationStatus.SENT)
                .used(false)
                .scanned(false)
                .build());

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        List<CheckInResult> results = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    CheckInRequestDto req = CheckInRequestDto.builder()
                            .token(concInv.getUniqueToken())
                            .eventId(mvpEvent.getId())
                            .scannerId("Main entrance")
                            .build();

                    CheckInResponseDto resp = invitationService.verifyInvitation(req);
                    results.add(resp.getResult());

                    if (resp.getResult() == CheckInResult.SUCCESS) {
                        successCount.incrementAndGet();
                    } else if (resp.getResult() == CheckInResult.ALREADY_USED) {
                        rejectedCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Fire simultaneous requests
        boolean completed = finishLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Concurrency test timed out");
        assertEquals(1, successCount.get(), "EXACTLY ONE scan must succeed!");
        assertEquals(1, rejectedCount.get(), "THE OTHER scan must be rejected as ALREADY_USED!");

        Invitation finalState = invitationRepository.findById(concInv.getId()).orElseThrow();
        assertTrue(finalState.isUsed());
        assertEquals(InvitationStatus.USED, finalState.getStatus());
    }
}
