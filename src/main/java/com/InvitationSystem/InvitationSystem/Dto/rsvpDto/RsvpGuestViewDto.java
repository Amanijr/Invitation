package com.InvitationSystem.InvitationSystem.Dto.rsvpDto;

import com.InvitationSystem.InvitationSystem.entity.RsvpStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RsvpGuestViewDto {
    private String token;
    private String guestName;
    private String eventName;
    private String venue;
    private LocalDateTime eventDate;
    private String stayDetails;
    private String registryUrl;
    private String registryLabel;
    private boolean askDietary;
    private boolean askMeal;
    private List<String> mealOptions;
    private RsvpStatus rsvpStatus;
    private Integer partySize;
    private String dietaryNotes;
    private String mealChoice;
    private boolean checkedIn;
}
