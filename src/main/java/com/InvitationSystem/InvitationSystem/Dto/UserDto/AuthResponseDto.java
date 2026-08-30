package com.InvitationSystem.InvitationSystem.Dto.UserDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@io.swagger.v3.oas.annotations.media.Schema(description = "Signed-in user plus JWT for subsequent writes")
public class AuthResponseDto {

    private String token;
    @Builder.Default
    private String tokenType = "Bearer";
    private UUID userId;
    private String firstName;
    private String lastName;
    private String email;
    private String role;

    public static AuthResponseDto from(String token, UserResponseDto user) {
        return AuthResponseDto.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getUserId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
