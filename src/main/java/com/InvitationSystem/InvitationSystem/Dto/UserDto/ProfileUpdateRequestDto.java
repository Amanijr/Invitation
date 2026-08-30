package com.InvitationSystem.InvitationSystem.Dto.UserDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@io.swagger.v3.oas.annotations.media.Schema(description = "Signed-in user profile update. Role cannot be changed here.")
public class ProfileUpdateRequestDto {

    private String firstName;
    private String lastName;
    private String email;
    private String currentPassword;
    private String newPassword;
}
