package com.InvitationSystem.InvitationSystem.Dto.templatesDto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplatePreviewRequestDto {

    private String guestName;
    private String eventName;
    private String eventDate;
    private String eventTime;
    private String eventVenue;
    private String sampleQrData;

    private List<TemplateFieldConfigDto> fieldConfigs;

    public Map<String, String> toSampleDataMap() {
        return Map.of(
                "GUEST_NAME", guestName != null ? guestName : "John & Jane Doe",
                "EVENT_NAME", eventName != null ? eventName : "Annual Grand Celebration",
                "EVENT_DATE", eventDate != null ? eventDate : "11 September 2026",
                "EVENT_TIME", eventTime != null ? eventTime : "6:00 PM",
                "EVENT_VENUE", eventVenue != null ? eventVenue : "The Grand Palace Hall, NY",
                "QR_CODE", sampleQrData != null ? sampleQrData : "SAMPLE-INVITATION-TOKEN-12345"
        );
    }
}
