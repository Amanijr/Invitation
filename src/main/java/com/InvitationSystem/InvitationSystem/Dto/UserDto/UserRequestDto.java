package com.InvitationSystem.InvitationSystem.Dto.UserDto;


import com.InvitationSystem.InvitationSystem.entity.UserRole;
import lombok.Data;

@Data
public class UserRequestDto {

    private  String firstName;
    private  String lastName;
    private  String email;
    private  String password;
    private String role;


}
