package com.InvitationSystem.InvitationSystem.Dto.guestDto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuestImportRowDto {

    private int rowNumber;
    private String fullName;
    private String phone;
    private String email;
    private boolean valid;
    private boolean duplicate;
    private List<String> errors;
}
