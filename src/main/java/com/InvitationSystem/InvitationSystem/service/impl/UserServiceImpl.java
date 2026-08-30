package com.InvitationSystem.InvitationSystem.service.impl;

import com.InvitationSystem.InvitationSystem.Dto.UserDto.ProfileUpdateRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.UserDto.UserRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.UserDto.UserResponseDto;
import com.InvitationSystem.InvitationSystem.entity.User;
import com.InvitationSystem.InvitationSystem.entity.UserRole;
import com.InvitationSystem.InvitationSystem.repository.UserRepository;
import com.InvitationSystem.InvitationSystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserResponseDto createUser(UserRequestDto request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("User with email " + request.getEmail() + " already exists");
        }

        UserRole role;
        try {
            String roleName = request.getRole() == null || request.getRole().isBlank()
                    ? UserRole.EVENT_MANAGER.name()
                    : request.getRole();
            role = UserRole.valueOf(roleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + request.getRole());
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);
        return mapToResponseDto(savedUser);
    }

    @Override
    public UserResponseDto getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        return mapToResponseDto(user);
    }

    @Override
    public Optional<UserResponseDto> getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(this::mapToResponseDto);
    }

    @Override
    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public UserResponseDto updateUser(UUID userId, UserRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        user.setEmail(request.getEmail());
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getRole() != null && !request.getRole().isBlank()) {
            try {
                user.setRole(UserRole.valueOf(request.getRole().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid role: " + request.getRole());
            }
        }

        User updatedUser = userRepository.save(user);
        return mapToResponseDto(updatedUser);
    }

    @Override
    public UserResponseDto updateProfile(String email, ProfileUpdateRequestDto request) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Sign in to edit your profile.");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found for " + email));

        String firstName = request.getFirstName() == null ? "" : request.getFirstName().trim();
        String lastName = request.getLastName() == null ? "" : request.getLastName().trim();
        String nextEmail = request.getEmail() == null ? "" : request.getEmail().trim();
        if (firstName.isEmpty()) {
            throw new IllegalArgumentException("First name is required.");
        }
        if (lastName.isEmpty()) {
            throw new IllegalArgumentException("Last name is required.");
        }
        if (nextEmail.isEmpty() || !nextEmail.contains("@")) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }

        boolean emailChanged = !user.getEmail().equalsIgnoreCase(nextEmail);
        boolean passwordChange = request.getNewPassword() != null && !request.getNewPassword().isBlank();
        if (emailChanged || passwordChange) {
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
                throw new IllegalArgumentException("Enter your current password to change email or password.");
            }
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                throw new IllegalArgumentException("Current password is incorrect.");
            }
        }
        if (passwordChange && request.getNewPassword().length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters.");
        }
        if (emailChanged && userRepository.existsByEmail(nextEmail)) {
            throw new IllegalArgumentException("Email already exists");
        }

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(nextEmail);
        if (passwordChange) {
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        }

        return mapToResponseDto(userRepository.save(user));
    }

    @Override
    public void deleteUser(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
        userRepository.deleteById(userId);
    }

    @Override
    public Optional<UserResponseDto> authenticate(String email, String password) {
        return userRepository.findByEmail(email)
                .filter(user -> passwordEncoder.matches(password, user.getPasswordHash()))
                .map(this::mapToResponseDto);
    }

    private UserResponseDto mapToResponseDto(User user) {
        UserResponseDto dto = new UserResponseDto();
        dto.setUserId(user.getUserId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().toString());
        dto.setEnabled(user.isEnabled());
        return dto;
    }


}
