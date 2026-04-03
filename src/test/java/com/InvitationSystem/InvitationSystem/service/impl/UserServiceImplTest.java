package com.InvitationSystem.InvitationSystem.service.impl;

import com.InvitationSystem.InvitationSystem.Dto.UserDto.UserRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.UserDto.UserResponseDto;
import com.InvitationSystem.InvitationSystem.entity.User;
import com.InvitationSystem.InvitationSystem.entity.UserRole;
import com.InvitationSystem.InvitationSystem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private UUID userId;
    private User user;
    private UserRequestDto requestDto;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        requestDto = new UserRequestDto("John", "Doe", "john@example.com", "secret", "EVENT_MANAGER");

        user = User.builder()
                .userId(userId)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .passwordHash("encoded-secret")
                .role(UserRole.EVENT_MANAGER)
                .enabled(true)
                .build();
    }

    @Test
    void createUser_success() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponseDto response = userService.createUser(requestDto);

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals("John", response.getFirstName());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void createUser_duplicateEmail_throws() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.createUser(requestDto));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUserById_success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserResponseDto response = userService.getUserById(userId);

        assertEquals("john@example.com", response.getEmail());
    }

    @Test
    void getUserById_notFound_throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> userService.getUserById(userId));
    }

    @Test
    void authenticate_success() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "encoded-secret")).thenReturn(true);

        Optional<UserResponseDto> response = userService.authenticate("john@example.com", "secret");

        assertTrue(response.isPresent());
        assertEquals(userId, response.get().getUserId());
    }

    @Test
    void authenticate_invalidPassword_returnsEmpty() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded-secret")).thenReturn(false);

        Optional<UserResponseDto> response = userService.authenticate("john@example.com", "wrong");

        assertFalse(response.isPresent());
    }

    @Test
    void updateUser_success() {
        UserRequestDto update = new UserRequestDto("Jane", "Smith", "jane@example.com", "newSecret", "ADMIN");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(passwordEncoder.encode("newSecret")).thenReturn("encoded-new");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDto response = userService.updateUser(userId, update);

        assertEquals("Jane", response.getFirstName());
        assertEquals("ADMIN", response.getRole());
    }

    @Test
    void deleteUser_notFound_throws() {
        when(userRepository.existsById(userId)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> userService.deleteUser(userId));
    }

    @Test
    void getAllUsers_success() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserResponseDto> users = userService.getAllUsers();

        assertEquals(1, users.size());
        assertEquals("john@example.com", users.get(0).getEmail());
    }
}
