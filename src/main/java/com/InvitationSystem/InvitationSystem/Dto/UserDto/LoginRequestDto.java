package com.InvitationSystem.InvitationSystem.Dto.UserDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@io.swagger.v3.oas.annotations.media.Schema(description = "Email and password for desk sign-in")
public class LoginRequestDto {

    private String email;
    private String password;
}
