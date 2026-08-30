package com.InvitationSystem.InvitationSystem.Dto.guestDto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuestImportPreviewDto {

    private UUID eventId;
    private String fileName;
    private String deliveryChannel;
    private int totalRows;
    private int validCount;
    private int invalidCount;
    private int duplicateCount;
    private List<GuestImportRowDto> rows;
}
