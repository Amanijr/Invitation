package com.InvitationSystem.InvitationSystem.controller;

import com.InvitationSystem.InvitationSystem.Dto.UserDto.LoginRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.UserDto.UserRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.UserDto.UserResponseDto;
import com.InvitationSystem.InvitationSystem.security.JwtGenerator;
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

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private JwtGenerator jwtGenerator;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UserResponseDto user;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();
        user = new UserResponseDto(
                UUID.randomUUID(),
                "Amani",
                "Juma",
                "amani@studio.com",
                "EVENT_MANAGER",
                true
        );
    }

    @Test
    void login_returnsJwt() throws Exception {
        when(userService.authenticate("amani@studio.com", "secret")).thenReturn(Optional.of(user));
        when(jwtGenerator.generateToken("amani@studio.com", user.getUserId(), "EVENT_MANAGER")).thenReturn("desk-jwt");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDto("amani@studio.com", "secret"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("desk-jwt"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.email").value("amani@studio.com"))
                .andExpect(jsonPath("$.userId").value(user.getUserId().toString()));
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        when(userService.authenticate("amani@studio.com", "wrong")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDto("amani@studio.com", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void register_returnsJwt() throws Exception {
        UserRequestDto request = new UserRequestDto("Amani", "Juma", "amani@studio.com", "secret", "EVENT_MANAGER");
        when(userService.createUser(any(UserRequestDto.class))).thenReturn(user);
        when(jwtGenerator.generateToken("amani@studio.com", user.getUserId(), "EVENT_MANAGER")).thenReturn("desk-jwt");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("desk-jwt"))
                .andExpect(jsonPath("$.email").value("amani@studio.com"));
    }
}
