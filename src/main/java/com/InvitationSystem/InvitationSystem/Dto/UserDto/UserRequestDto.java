package com.InvitationSystem.InvitationSystem.Dto.UserDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@io.swagger.v3.oas.annotations.media.Schema(description = "Request payload for creating or updating users")
public class UserRequestDto {

    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String role;
}
