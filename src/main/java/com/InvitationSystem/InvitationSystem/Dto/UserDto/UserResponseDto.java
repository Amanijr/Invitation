package com.InvitationSystem.InvitationSystem.Dto.UserDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@io.swagger.v3.oas.annotations.media.Schema(description = "Response payload for user details")
public class UserResponseDto {

    private UUID userId;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private boolean enabled;
}
