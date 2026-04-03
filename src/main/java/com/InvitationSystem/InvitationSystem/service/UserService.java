package com.InvitationSystem.InvitationSystem.service;

import com.InvitationSystem.InvitationSystem.Dto.UserDto.UserRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.UserDto.UserResponseDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserService {

    UserResponseDto createUser(UserRequestDto request);

    UserResponseDto getUserById(UUID userId);

    Optional<UserResponseDto> getUserByEmail(String email);

    List<UserResponseDto> getAllUsers();

    UserResponseDto updateUser(UUID userId, UserRequestDto request);

    void deleteUser(UUID userId);

    Optional<UserResponseDto> authenticate(String email, String password);
}
