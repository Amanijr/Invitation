package com.InvitationSystem.InvitationSystem.Dto.bulkUploadDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@io.swagger.v3.oas.annotations.media.Schema(description = "Request payload for processing a bulk upload file")
public class BulkUploadRequestDto {

    private UUID eventId;
    private UUID templateId;
    private String fileType; // "EXCEL" or "PDF"
    private byte[] fileContent;
    private String fileName;
}
