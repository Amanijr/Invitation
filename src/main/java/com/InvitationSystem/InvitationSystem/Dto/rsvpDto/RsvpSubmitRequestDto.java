package com.InvitationSystem.InvitationSystem.Dto.rsvpDto;

import com.InvitationSystem.InvitationSystem.entity.RsvpStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RsvpSubmitRequestDto {
    private RsvpStatus status;
    private Integer partySize;
    private String dietaryNotes;
    private String mealChoice;
}
