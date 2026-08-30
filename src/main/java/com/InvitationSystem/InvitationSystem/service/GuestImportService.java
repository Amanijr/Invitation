package com.InvitationSystem.InvitationSystem.service;

import com.InvitationSystem.InvitationSystem.Dto.guestDto.GuestImportConfirmRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.guestDto.GuestImportPreviewDto;
import com.InvitationSystem.InvitationSystem.Dto.guestDto.GuestImportSummaryDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface GuestImportService {

    /**
     * Parses spreadsheet file (CSV, XLSX, XLS) and runs validation without writing to DB.
     */
    GuestImportPreviewDto previewImport(MultipartFile file, UUID eventId, String deliveryChannel);

    /**
     * Parses spreadsheet bytes and runs validation.
     */
    GuestImportPreviewDto previewImportBytes(byte[] fileBytes, String fileName, UUID eventId, String deliveryChannel);

    /**
     * Confirms and persists valid rows into the database atomically.
     */
    GuestImportSummaryDto confirmImport(GuestImportConfirmRequestDto request);
}
