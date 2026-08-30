package com.InvitationSystem.InvitationSystem.service.impl;

import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.BatchRenderResultDto;
import com.InvitationSystem.InvitationSystem.Dto.templatesDto.TemplateFieldConfigDto;
import com.InvitationSystem.InvitationSystem.entity.*;
import com.InvitationSystem.InvitationSystem.repository.EventRepository;
import com.InvitationSystem.InvitationSystem.repository.GuestRepository;
import com.InvitationSystem.InvitationSystem.repository.InvitationRepository;
import com.InvitationSystem.InvitationSystem.repository.TemplateRepository;
import com.InvitationSystem.InvitationSystem.service.TemplateFieldConfigService;
import com.InvitationSystem.InvitationSystem.service.TemplateService;
import com.InvitationSystem.InvitationSystem.service.storage.FileMetadata;
import com.InvitationSystem.InvitationSystem.service.storage.FileStorageService;
import com.InvitationSystem.InvitationSystem.util.ImageCardGeneratorService;
import com.InvitationSystem.InvitationSystem.util.QRCodeService;
import com.InvitationSystem.InvitationSystem.util.TokenGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ECardRenderingEngineServiceImplTest {

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private GuestRepository guestRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TemplateRepository templateRepository;

    @Mock
    private TemplateService templateService;

    @Mock
    private TemplateFieldConfigService fieldConfigService;

    @Mock
    private ImageCardGeneratorService imageCardGeneratorService;

    @Mock
    private QRCodeService qrCodeService;

    @Mock
    private TokenGeneratorService tokenGeneratorService;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private ECardRenderingEngineServiceImpl eCardRenderingEngineService;

    private UUID invitationIdA;
    private UUID invitationIdB;
    private UUID guestIdA;
    private UUID guestIdB;
    private UUID eventId;
    private UUID templateId;

    private Invitation invitationA;
    private Invitation invitationB;
    private Guest guestA;
    private Guest guestB;
    private Event event;
    private Template template;

    @BeforeEach
    void setUp() {
        invitationIdA = UUID.randomUUID();
        invitationIdB = UUID.randomUUID();
        guestIdA = UUID.randomUUID();
        guestIdB = UUID.randomUUID();
        eventId = UUID.randomUUID();
        templateId = UUID.randomUUID();

        guestA = Guest.builder()
                .id(guestIdA)
                .eventId(eventId)
                .fullName("John Michael")
                .email("john@example.com")
                .build();

        guestB = Guest.builder()
                .id(guestIdB)
                .eventId(eventId)
                .fullName("Sarah Vance")
                .email("sarah@example.com")
                .build();

        event = Event.builder()
                .id(eventId)
                .eventName("Amani & Sarah Wedding")
                .venue("The Grand Palace")
                .eventDate(LocalDateTime.of(2026, 10, 14, 18, 0))
                .eventType(EventType.WEDDING)
                .build();

        template = Template.builder()
                .id(templateId)
                .templateName("Wedding Classic")
                .storagePath("WEDDING/template.png")
                .width(1920)
                .height(1080)
                .build();

        invitationA = Invitation.builder()
                .id(invitationIdA)
                .eventId(eventId)
                .guestId(guestIdA)
                .templateId(templateId)
                .recipientEmail("john@example.com")
                .status(InvitationStatus.GENERATED)
                .build();

        invitationB = Invitation.builder()
                .id(invitationIdB)
                .eventId(eventId)
                .guestId(guestIdB)
                .templateId(templateId)
                .recipientEmail("sarah@example.com")
                .status(InvitationStatus.GENERATED)
                .build();
    }

    @Test
    void renderAndStoreCard_singleInvitation_success() {
        when(tokenGeneratorService.generateSecureToken()).thenReturn("TOKEN-1111");
        when(qrCodeService.generateQRCodeImage(anyString())).thenReturn("BASE64-QR-1111");
        when(guestRepository.findById(guestIdA)).thenReturn(Optional.of(guestA));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(fieldConfigService.getFieldConfigsByTemplateId(templateId)).thenReturn(List.of());
        when(imageCardGeneratorService.renderCardImage(any(), any(), any())).thenReturn("DUMMY-PNG-BYTES".getBytes());

        FileMetadata metadata = FileMetadata.builder()
                .storagePath("CARDS/ecard-random-uuid-1.png")
                .build();
        doReturn(metadata).when(fileStorageService).storeFile(any(), anyString());
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(i -> i.getArgument(0));

        Invitation result = eCardRenderingEngineService.renderAndStoreCard(invitationA);

        assertNotNull(result);
        assertEquals("TOKEN-1111", result.getUniqueToken());
        assertEquals("BASE64-QR-1111", result.getQrCode());
        assertEquals("CARDS/ecard-random-uuid-1.png", result.getCardReference());
        verify(fileStorageService, times(1)).storeFile(any(), eq("CARDS"));
    }

    @Test
    void renderAndStoreCard_stampsOnlyGuestNameAndQr() {
        TemplateFieldConfigDto dateSlot = TemplateFieldConfigDto.builder()
                .fieldType(FieldType.EVENT_DATE)
                .x(10.0).y(10.0).width(80.0).height(10.0)
                .fontSize(24).fontColor("#000000").alignment("CENTER").fontWeight("NORMAL")
                .build();
        TemplateFieldConfigDto nameSlot = TemplateFieldConfigDto.builder()
                .fieldType(FieldType.GUEST_NAME)
                .x(12.0).y(28.0).width(76.0).height(8.0)
                .fontSize(32).fontColor("#111318").alignment("CENTER").fontWeight("BOLD")
                .build();
        TemplateFieldConfigDto qrSlot = TemplateFieldConfigDto.builder()
                .fieldType(FieldType.QR_CODE)
                .x(38.0).y(78.0).width(24.0).height(18.0)
                .build();

        when(tokenGeneratorService.generateSecureToken()).thenReturn("TOKEN-1111");
        when(qrCodeService.generateQRCodeImage(anyString())).thenReturn("BASE64-QR-1111");
        when(guestRepository.findById(guestIdA)).thenReturn(Optional.of(guestA));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(fieldConfigService.getFieldConfigsByTemplateId(templateId)).thenReturn(List.of(dateSlot, nameSlot, qrSlot));
        when(imageCardGeneratorService.renderCardImage(any(), argThat(list ->
                list != null
                        && list.size() == 2
                        && list.stream().allMatch(config -> config.getFieldType() == FieldType.GUEST_NAME
                        || config.getFieldType() == FieldType.QR_CODE)
        ), any())).thenReturn("DUMMY-PNG-BYTES".getBytes());

        FileMetadata metadata = FileMetadata.builder().storagePath("CARDS/ecard-name-qr.png").build();
        doReturn(metadata).when(fileStorageService).storeFile(any(), anyString());
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(i -> i.getArgument(0));

        Invitation result = eCardRenderingEngineService.renderAndStoreCard(invitationA);

        assertEquals("CARDS/ecard-name-qr.png", result.getCardReference());
        verify(imageCardGeneratorService).renderCardImage(any(), argThat(list -> list.size() == 2), any());
    }

    @Test
    void renderAndStoreCard_uniqueQrPerInvitation_demonstration() {
        // Guest A
        when(tokenGeneratorService.generateSecureToken())
                .thenReturn("TOKEN-GUEST-A")
                .thenReturn("TOKEN-GUEST-B");

        when(qrCodeService.generateQRCodeImage(contains("TOKEN-GUEST-A"))).thenReturn("BASE64-QR-A");
        when(qrCodeService.generateQRCodeImage(contains("TOKEN-GUEST-B"))).thenReturn("BASE64-QR-B");

        when(guestRepository.findById(guestIdA)).thenReturn(Optional.of(guestA));
        when(guestRepository.findById(guestIdB)).thenReturn(Optional.of(guestB));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(fieldConfigService.getFieldConfigsByTemplateId(templateId)).thenReturn(List.of());
        when(imageCardGeneratorService.renderCardImage(any(), any(), any())).thenReturn("PNG-BYTES".getBytes());

        FileMetadata metaA = FileMetadata.builder().storagePath("CARDS/card-a.png").build();
        FileMetadata metaB = FileMetadata.builder().storagePath("CARDS/card-b.png").build();

        doReturn(metaA).doReturn(metaB).when(fileStorageService).storeFile(any(), anyString());

        when(invitationRepository.save(any(Invitation.class))).thenAnswer(i -> i.getArgument(0));

        Invitation cardA = eCardRenderingEngineService.renderAndStoreCard(invitationA);
        Invitation cardB = eCardRenderingEngineService.renderAndStoreCard(invitationB);

        // Assert Guest A -> Card A -> QR A
        assertNotNull(cardA);
        assertEquals("TOKEN-GUEST-A", cardA.getUniqueToken());
        assertEquals("BASE64-QR-A", cardA.getQrCode());
        assertEquals("CARDS/card-a.png", cardA.getCardReference());

        // Assert Guest B -> Card B -> QR B
        assertNotNull(cardB);
        assertEquals("TOKEN-GUEST-B", cardB.getUniqueToken());
        assertEquals("BASE64-QR-B", cardB.getQrCode());
        assertEquals("CARDS/card-b.png", cardB.getCardReference());

        // CRITICAL REQUIREMENT ASSERTION: QR A != QR B
        assertNotEquals(cardA.getUniqueToken(), cardB.getUniqueToken());
        assertNotEquals(cardA.getQrCode(), cardB.getQrCode());
        assertNotEquals(cardA.getCardReference(), cardB.getCardReference());
    }

    @Test
    void renderBatch_handlesPartialFailuresGracefully() {
        when(invitationRepository.findById(invitationIdA)).thenReturn(Optional.of(invitationA));
        when(invitationRepository.findById(invitationIdB)).thenReturn(Optional.of(invitationB));

        when(tokenGeneratorService.generateSecureToken()).thenReturn("TOKEN-1");
        when(qrCodeService.generateQRCodeImage(anyString())).thenReturn("QR-1");
        when(guestRepository.findById(guestIdA)).thenReturn(Optional.of(guestA));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(fieldConfigService.getFieldConfigsByTemplateId(templateId)).thenReturn(List.of());
        when(imageCardGeneratorService.renderCardImage(any(), any(), any()))
                .thenAnswer(i -> "PNG-BYTES".getBytes()) // First succeeds
                .thenThrow(new RuntimeException("Rendering glitch for B")); // Second fails

        FileMetadata metaA = FileMetadata.builder().storagePath("CARDS/card-a.png").build();
        doReturn(metaA).when(fileStorageService).storeFile(any(), anyString());
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(i -> i.getArgument(0));

        BatchRenderResultDto result = eCardRenderingEngineService.renderBatch(List.of(invitationIdA, invitationIdB));

        assertNotNull(result);
        assertEquals(2, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(1, result.getFailedInvitationIds().size());
        assertEquals(invitationIdB, result.getFailedInvitationIds().get(0));
    }
}
