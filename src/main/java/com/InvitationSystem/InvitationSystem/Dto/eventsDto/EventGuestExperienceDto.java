package com.InvitationSystem.InvitationSystem.Dto.eventsDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@io.swagger.v3.oas.annotations.media.Schema(description = "Stay, registry, and survey questions shown on the guest RSVP page")
public class EventGuestExperienceDto {
    private String stayDetails;
    private String registryUrl;
    private String registryLabel;
    private Boolean askDietary;
    private Boolean askMeal;
    private String mealOptions;
}
