package com.InvitationSystem.InvitationSystem.service.impl;

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

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.valueOf(request.getRole().toUpperCase()))
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
        try {
            user.setRole(UserRole.valueOf(request.getRole().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + request.getRole());
        }

        User updatedUser = userRepository.save(user);
        return mapToResponseDto(updatedUser);
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
