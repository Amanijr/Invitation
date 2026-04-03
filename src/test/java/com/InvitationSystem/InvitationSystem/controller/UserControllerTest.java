package com.InvitationSystem.InvitationSystem.controller;

import com.InvitationSystem.InvitationSystem.Dto.UserDto.UserRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.UserDto.UserResponseDto;
import com.InvitationSystem.InvitationSystem.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private UUID userId;
    private UserResponseDto responseDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        objectMapper = new ObjectMapper();

        userId = UUID.randomUUID();
        responseDto = new UserResponseDto(
                userId,
                "John",
                "Doe",
                "john@example.com",
                "EVENT_MANAGER",
                true
        );
    }

    @Test
    void getUserById_success() throws Exception {
        when(userService.getUserById(userId)).thenReturn(responseDto);

        mockMvc.perform(get("/api/v1/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.firstName").value("John"));

        verify(userService, times(1)).getUserById(userId);
    }

    @Test
    void getUserByEmail_success() throws Exception {
        when(userService.getUserByEmail("john@example.com")).thenReturn(Optional.of(responseDto));

        mockMvc.perform(get("/api/v1/users/email/{email}", "john@example.com")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    void getUserByEmail_notFound() throws Exception {
        when(userService.getUserByEmail("missing@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/users/email/{email}", "missing@example.com")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllUsers_success() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void updateUser_success() throws Exception {
        UserRequestDto requestDto = new UserRequestDto("Jane", "Doe", "jane@example.com", "secret", "ADMIN");
        when(userService.updateUser(eq(userId), any(UserRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(put("/api/v1/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    void deleteUser_success() throws Exception {
        doNothing().when(userService).deleteUser(userId);

        mockMvc.perform(delete("/api/v1/users/{userId}", userId))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).deleteUser(userId);
    }
}
