package com.InvitationSystem.InvitationSystem.controller;

import com.InvitationSystem.InvitationSystem.Dto.guestDto.*;
import com.InvitationSystem.InvitationSystem.entity.User;
import com.InvitationSystem.InvitationSystem.entity.UserRole;
import com.InvitationSystem.InvitationSystem.exception.GlobalExceptionHandler;
import com.InvitationSystem.InvitationSystem.security.DeskUsers;
import com.InvitationSystem.InvitationSystem.service.EventService;
import com.InvitationSystem.InvitationSystem.service.GuestImportService;
import com.InvitationSystem.InvitationSystem.service.GuestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GuestControllerTest {

    @Mock
    private GuestService guestService;

    @Mock
    private GuestImportService guestImportService;

    @Mock
    private DeskUsers deskUsers;

    @Mock
    private EventService eventService;

    @InjectMocks
    private GuestController guestController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private UUID eventId;
    private UUID guestId;
    private GuestResponseDto responseDto;
    private User deskUser;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(guestController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        eventId = UUID.randomUUID();
        guestId = UUID.randomUUID();
        deskUser = User.builder()
                .userId(UUID.randomUUID())
                .firstName("Amani")
                .lastName("Juma")
                .email("amani@studio.com")
                .passwordHash("hash")
                .role(UserRole.EVENT_MANAGER)
                .build();
        authentication = new UsernamePasswordAuthenticationToken(deskUser.getEmail(), "n");

        responseDto = GuestResponseDto.builder()
                .id(guestId)
                .eventId(eventId)
                .fullName("John Doe")
                .email("john@example.com")
                .phone("+1234567890")
                .build();
    }

    @Test
    void createGuest_ValidRequest_Returns201Created() throws Exception {
        GuestRequestDto requestDto = GuestRequestDto.builder()
                .eventId(eventId)
                .fullName("John Doe")
                .email("john@example.com")
                .phone("+1234567890")
                .build();

        when(deskUsers.require(any(Authentication.class))).thenReturn(deskUser);
        doNothing().when(eventService).assertCanAccess(eventId, deskUser.getUserId(), deskUser.getRole());
        when(guestService.createGuest(any())).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/guests")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(guestId.toString()))
                .andExpect(jsonPath("$.fullName").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));

        verify(eventService).assertCanAccess(eventId, deskUser.getUserId(), deskUser.getRole());
    }

    @Test
    void createGuest_otherUsersEvent_returns403() throws Exception {
        GuestRequestDto requestDto = GuestRequestDto.builder()
                .eventId(eventId)
                .fullName("John Doe")
                .email("john@example.com")
                .phone("+1234567890")
                .build();

        when(deskUsers.require(any(Authentication.class))).thenReturn(deskUser);
        doThrow(new AccessDeniedException("Not authorized to manage this event"))
                .when(eventService).assertCanAccess(eventId, deskUser.getUserId(), deskUser.getRole());

        mockMvc.perform(post("/api/v1/guests")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isForbidden());

        verify(guestService, never()).createGuest(any());
    }

    @Test
    void getGuestById_ReturnsGuest() throws Exception {
        when(deskUsers.require(any(Authentication.class))).thenReturn(deskUser);
        doNothing().when(eventService).assertCanAccess(eventId, deskUser.getUserId(), deskUser.getRole());
        when(guestService.getGuestById(guestId)).thenReturn(responseDto);

        mockMvc.perform(get("/api/v1/guests/{guestId}", guestId).principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(guestId.toString()))
                .andExpect(jsonPath("$.fullName").value("John Doe"));
    }

    @Test
    void getGuestsByEvent_ReturnsList() throws Exception {
        when(deskUsers.require(any(Authentication.class))).thenReturn(deskUser);
        doNothing().when(eventService).assertCanAccess(eventId, deskUser.getUserId(), deskUser.getRole());
        when(guestService.getGuestsByEvent(eventId)).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/v1/guests/event/{eventId}", eventId).principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fullName").value("John Doe"));
    }

    @Test
    void getAllGuests_ReturnsList() throws Exception {
        when(deskUsers.require(any(Authentication.class))).thenReturn(deskUser);
        when(guestService.getGuestsForActor(deskUser.getUserId(), deskUser.getRole()))
                .thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/v1/guests").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fullName").value("John Doe"));
    }

    @Test
    void searchGuests_ReturnsMatchingGuests() throws Exception {
        when(deskUsers.require(any(Authentication.class))).thenReturn(deskUser);
        doNothing().when(eventService).assertCanAccess(eventId, deskUser.getUserId(), deskUser.getRole());
        when(guestService.searchGuests(eventId, "John")).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/v1/guests/search")
                        .param("eventId", eventId.toString())
                        .param("query", "John")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fullName").value("John Doe"));
    }

    @Test
    void updateGuest_ReturnsUpdatedGuest() throws Exception {
        GuestRequestDto requestDto = GuestRequestDto.builder()
                .eventId(eventId)
                .fullName("John Updated")
                .email("john@example.com")
                .phone("+1234567890")
                .build();

        GuestResponseDto updatedDto = GuestResponseDto.builder()
                .id(guestId)
                .eventId(eventId)
                .fullName("John Updated")
                .email("john@example.com")
                .phone("+1234567890")
                .build();

        when(deskUsers.require(any(Authentication.class))).thenReturn(deskUser);
        doNothing().when(eventService).assertCanAccess(eventId, deskUser.getUserId(), deskUser.getRole());
        when(guestService.getGuestById(guestId)).thenReturn(responseDto);
        when(guestService.updateGuest(eq(guestId), any())).thenReturn(updatedDto);

        mockMvc.perform(put("/api/v1/guests/{guestId}", guestId)
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("John Updated"));
    }

    @Test
    void deleteGuest_ReturnsNoContent() throws Exception {
        when(deskUsers.require(any(Authentication.class))).thenReturn(deskUser);
        doNothing().when(eventService).assertCanAccess(eventId, deskUser.getUserId(), deskUser.getRole());
        when(guestService.getGuestById(guestId)).thenReturn(responseDto);
        doNothing().when(guestService).deleteGuest(guestId);

        mockMvc.perform(delete("/api/v1/guests/{guestId}", guestId).principal(authentication))
                .andExpect(status().isNoContent());

        verify(guestService, times(1)).deleteGuest(guestId);
    }

    @Test
    void previewImport_ReturnsPreviewDto() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "guests.csv", "text/csv", "name,phone,email\nJohn,+1234,john@ex.com".getBytes());

        GuestImportPreviewDto previewDto = GuestImportPreviewDto.builder()
                .eventId(eventId)
                .fileName("guests.csv")
                .deliveryChannel("BOTH")
                .totalRows(1)
                .validCount(1)
                .invalidCount(0)
                .duplicateCount(0)
                .rows(List.of())
                .build();

        when(deskUsers.require(any(Authentication.class))).thenReturn(deskUser);
        doNothing().when(eventService).assertCanAccess(eventId, deskUser.getUserId(), deskUser.getRole());
        when(guestImportService.previewImport(any(), eq(eventId), eq("BOTH"))).thenReturn(previewDto);

        mockMvc.perform(multipart("/api/v1/guests/import/preview")
                        .file(file)
                        .principal(authentication)
                        .param("eventId", eventId.toString())
                        .param("deliveryChannel", "BOTH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("guests.csv"))
                .andExpect(jsonPath("$.validCount").value(1));
    }

    @Test
    void confirmImport_ReturnsSummaryDto() throws Exception {
        GuestImportConfirmRequestDto requestDto = GuestImportConfirmRequestDto.builder()
                .eventId(eventId)
                .deliveryChannel("BOTH")
                .rowsToImport(List.of())
                .build();

        GuestImportSummaryDto summaryDto = GuestImportSummaryDto.builder()
                .eventId(eventId)
                .importedCount(5)
                .skippedCount(1)
                .importedGuests(List.of())
                .build();

        when(deskUsers.require(any(Authentication.class))).thenReturn(deskUser);
        doNothing().when(eventService).assertCanAccess(eventId, deskUser.getUserId(), deskUser.getRole());
        when(guestImportService.confirmImport(any())).thenReturn(summaryDto);

        mockMvc.perform(post("/api/v1/guests/import/confirm")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.importedCount").value(5))
                .andExpect(jsonPath("$.skippedCount").value(1));
    }
}
