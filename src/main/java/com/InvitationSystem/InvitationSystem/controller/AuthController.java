package com.InvitationSystem.InvitationSystem.controller;

import com.InvitationSystem.InvitationSystem.Dto.UserDto.AuthResponseDto;
import com.InvitationSystem.InvitationSystem.Dto.UserDto.LoginRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.UserDto.UserRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.UserDto.UserResponseDto;
import com.InvitationSystem.InvitationSystem.security.JwtGenerator;
import com.InvitationSystem.InvitationSystem.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and registration endpoints")
public class AuthController {

    private final JwtGenerator jwtGenerator;
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> registerUser(@RequestBody UserRequestDto request) {
        UserResponseDto user = userService.createUser(request);
        String token = jwtGenerator.generateToken(user.getEmail(), user.getUserId(), user.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(AuthResponseDto.from(token, user));
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequestDto request) {
        Optional<UserResponseDto> user = userService.authenticate(request.getEmail(), request.getPassword());
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid credentials"));
        }
        String token = jwtGenerator.generateToken(user.get().getEmail(), user.get().getUserId(), user.get().getRole());
        return ResponseEntity.ok(AuthResponseDto.from(token, user.get()));
    }
}
