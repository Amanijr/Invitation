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
public class RsvpTrackingDto {
    private long total;
    private long opened;
    private long going;
    private long notGoing;
    private long maybe;
    private long noReply;
    private List<RsvpGuestRowDto> guests;
    private List<RsvpActivityDto> activity;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RsvpGuestRowDto {
        private String guestName;
        private String token;
        private RsvpStatus rsvpStatus;
        private LocalDateTime openedAt;
        private LocalDateTime rsvpAt;
        private Integer partySize;
        private String dietaryNotes;
        private String mealChoice;
        private String lastActivity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RsvpActivityDto {
        private String guestName;
        private String text;
        private String status;
        private LocalDateTime at;
    }
}
