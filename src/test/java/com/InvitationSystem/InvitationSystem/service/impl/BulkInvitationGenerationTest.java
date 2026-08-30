package com.InvitationSystem.InvitationSystem.service.impl;

import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.BulkGenerationRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.BulkGenerationResultDto;
import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.RegenerationPolicy;
import com.InvitationSystem.InvitationSystem.entity.*;
import com.InvitationSystem.InvitationSystem.repository.*;
import com.InvitationSystem.InvitationSystem.service.ECardRenderingEngineService;
import com.InvitationSystem.InvitationSystem.util.QRCodeService;
import com.InvitationSystem.InvitationSystem.util.TokenGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BulkInvitationGenerationTest {

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TemplateRepository templateRepository;

    @Mock
    private GuestRepository guestRepository;

    @Mock
    private TokenGeneratorService tokenGeneratorService;

    @Mock
    private QRCodeService qrCodeService;

    @Mock
    private ECardRenderingEngineService eCardRenderingEngineService;

    @InjectMocks
    private InvitationServiceImpl invitationService;

    private UUID eventId;
    private UUID templateId;
    private UUID guestId1;
    private UUID guestId2;
    private Guest guest1;
    private Guest guest2;
    private Event event;
    private Template template;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        templateId = UUID.randomUUID();
        guestId1 = UUID.randomUUID();
        guestId2 = UUID.randomUUID();

        event = Event.builder().id(eventId).eventName("Royal Gala").build();
        template = Template.builder().id(templateId).templateName("Luxury Template").active(true).version(1).build();

        guest1 = Guest.builder().id(guestId1).eventId(eventId).fullName("Alice Smith").email("alice@example.com").build();
        guest2 = Guest.builder().id(guestId2).eventId(eventId).fullName("Bob Jones").phone("+1234567890").build();
    }

    @Test
    void generateBulkInvitations_NewGuests_Success() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(guestRepository.findByEventId(eventId)).thenReturn(List.of(guest1, guest2));

        when(invitationRepository.findByEventIdAndGuestId(eventId, guestId1)).thenReturn(Optional.empty());
        when(invitationRepository.findByEventIdAndGuestId(eventId, guestId2)).thenReturn(Optional.empty());

        when(tokenGeneratorService.generateSecureToken()).thenReturn("TOKEN-1", "TOKEN-2");
        when(qrCodeService.generateQRCodeImage(any())).thenReturn("BASE64-QR");

        Invitation savedInv1 = Invitation.builder().id(UUID.randomUUID()).eventId(eventId).guestId(guestId1).uniqueToken("TOKEN-1").build();
        Invitation savedInv2 = Invitation.builder().id(UUID.randomUUID()).eventId(eventId).guestId(guestId2).uniqueToken("TOKEN-2").build();

        when(invitationRepository.save(any(Invitation.class))).thenReturn(savedInv1, savedInv2);
        when(eCardRenderingEngineService.renderAndStoreCard(any(Invitation.class))).thenAnswer(i -> i.getArgument(0));

        BulkGenerationRequestDto request = BulkGenerationRequestDto.builder()
                .eventId(eventId)
                .templateId(templateId)
                .regenerationPolicy(RegenerationPolicy.SKIP_EXISTING)
                .build();

        BulkGenerationResultDto result = invitationService.generateBulkInvitations(request);

        assertNotNull(result);
        assertEquals(2, result.getTotalGuests());
        assertEquals(2, result.getSuccessCount());
        assertEquals(0, result.getSkippedCount());
        assertEquals(0, result.getFailedCount());
        assertEquals(2, result.getSuccessfulInvitationIds().size());
        verify(invitationRepository, times(2)).save(any(Invitation.class));
    }

    @Test
    void generateBulkInvitations_SkipExisting_Skipped() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(guestRepository.findByEventId(eventId)).thenReturn(List.of(guest1));

        Invitation existing = Invitation.builder().id(UUID.randomUUID()).eventId(eventId).guestId(guestId1).build();
        when(invitationRepository.findByEventIdAndGuestId(eventId, guestId1)).thenReturn(Optional.of(existing));

        BulkGenerationRequestDto request = BulkGenerationRequestDto.builder()
                .eventId(eventId)
                .templateId(templateId)
                .regenerationPolicy(RegenerationPolicy.SKIP_EXISTING)
                .build();

        BulkGenerationResultDto result = invitationService.generateBulkInvitations(request);

        assertNotNull(result);
        assertEquals(1, result.getTotalGuests());
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getSkippedCount());
        verify(invitationRepository, never()).save(any());
    }

    @Test
    void generateBulkInvitations_RegenerateExisting_UpdatesExisting() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(guestRepository.findByEventId(eventId)).thenReturn(List.of(guest1));

        Invitation existing = Invitation.builder().id(UUID.randomUUID()).eventId(eventId).guestId(guestId1).uniqueToken("OLD-TOKEN").build();
        when(invitationRepository.findByEventIdAndGuestId(eventId, guestId1)).thenReturn(Optional.of(existing));
        when(eCardRenderingEngineService.renderAndStoreCard(any(Invitation.class))).thenReturn(existing);
        when(invitationRepository.save(any(Invitation.class))).thenReturn(existing);

        BulkGenerationRequestDto request = BulkGenerationRequestDto.builder()
                .eventId(eventId)
                .templateId(templateId)
                .regenerationPolicy(RegenerationPolicy.REGENERATE_EXISTING)
                .build();

        BulkGenerationResultDto result = invitationService.generateBulkInvitations(request);

        assertNotNull(result);
        assertEquals(1, result.getTotalGuests());
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getSkippedCount());
        verify(eCardRenderingEngineService, times(1)).renderAndStoreCard(existing);
    }

    @Test
    void generateBulkInvitations_GuestNoContactInfo_RecordsFailureAndContinues() {
        Guest guestNoContact = Guest.builder().id(UUID.randomUUID()).eventId(eventId).fullName("No Contact").build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(guestRepository.findByEventId(eventId)).thenReturn(List.of(guestNoContact));

        BulkGenerationRequestDto request = BulkGenerationRequestDto.builder()
                .eventId(eventId)
                .templateId(templateId)
                .regenerationPolicy(RegenerationPolicy.SKIP_EXISTING)
                .build();

        BulkGenerationResultDto result = invitationService.generateBulkInvitations(request);

        assertNotNull(result);
        assertEquals(1, result.getTotalGuests());
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailedCount());
        assertEquals(1, result.getErrors().size());
        assertEquals("No Contact", result.getErrors().get(0).getGuestName());
    }
}
