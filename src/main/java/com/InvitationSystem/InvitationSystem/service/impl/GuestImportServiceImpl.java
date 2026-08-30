package com.InvitationSystem.InvitationSystem.service.impl;

import com.InvitationSystem.InvitationSystem.Dto.guestDto.GuestImportConfirmRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.guestDto.GuestImportPreviewDto;
import com.InvitationSystem.InvitationSystem.Dto.guestDto.GuestImportRowDto;
import com.InvitationSystem.InvitationSystem.Dto.guestDto.GuestImportSummaryDto;
import com.InvitationSystem.InvitationSystem.Dto.guestDto.GuestResponseDto;
import com.InvitationSystem.InvitationSystem.entity.AdmissionType;
import com.InvitationSystem.InvitationSystem.entity.Event;
import com.InvitationSystem.InvitationSystem.entity.Guest;
import com.InvitationSystem.InvitationSystem.repository.EventRepository;
import com.InvitationSystem.InvitationSystem.repository.GuestRepository;
import com.InvitationSystem.InvitationSystem.service.GuestImportService;
import com.InvitationSystem.InvitationSystem.service.InvitationService;
import com.InvitationSystem.InvitationSystem.entity.AdmissionType;
import com.InvitationSystem.InvitationSystem.util.ExcelParserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class GuestImportServiceImpl implements GuestImportService {

    @Autowired
    private ExcelParserService excelParserService;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private InvitationService invitationService;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9\\s\\-]{7,20}$");

    @Override
    public GuestImportPreviewDto previewImport(MultipartFile file, UUID eventId, String deliveryChannel) {
        List<Map<String, String>> rawRows = excelParserService.parseFile(file);
        return processPreview(rawRows, file.getOriginalFilename(), eventId, deliveryChannel);
    }

    @Override
    public GuestImportPreviewDto previewImportBytes(byte[] fileBytes, String fileName, UUID eventId, String deliveryChannel) {
        List<Map<String, String>> rawRows = excelParserService.parseBytes(fileBytes, fileName);
        return processPreview(rawRows, fileName, eventId, deliveryChannel);
    }

    private GuestImportPreviewDto processPreview(List<Map<String, String>> rawRows, String fileName, UUID eventId, String deliveryChannel) {
        if (!eventRepository.existsById(eventId)) {
            throw new IllegalArgumentException("Event not found with ID: " + eventId);
        }

        String channel = (deliveryChannel != null) ? deliveryChannel.toUpperCase() : "BOTH";
        boolean requiresPhone = channel.contains("WHATSAPP") || channel.contains("SMS") || channel.contains("BOTH") || channel.contains("ALL");
        boolean requiresEmail = channel.contains("EMAIL") || channel.contains("BOTH") || channel.contains("ALL");

        List<GuestImportRowDto> processedRows = new ArrayList<>();
        Set<String> seenNamesInFile = new HashSet<>();
        Set<String> seenEmailsInFile = new HashSet<>();
        Set<String> seenPhonesInFile = new HashSet<>();

        int validCount = 0;
        int invalidCount = 0;
        int duplicateCount = 0;

        int rowNumIndex = 0;
        for (Map<String, String> rowMap : rawRows) {
            rowNumIndex++;
            int rowNum = rowMap.containsKey("_rowNumber") ? Integer.parseInt(rowMap.get("_rowNumber")) : rowNumIndex;
            
            String fullName = rowMap.getOrDefault("fullName", "").trim();
            if (fullName.isEmpty() && rowMap.containsKey("name")) fullName = rowMap.get("name").trim();

            String phone = rowMap.getOrDefault("phone", "").trim();
            String email = rowMap.getOrDefault("email", "").trim();

            List<String> errors = new ArrayList<>();
            boolean isDuplicate = false;

            // 1. Mandatory Name Check
            if (fullName.isEmpty()) {
                errors.add("Full name is required");
            }

            // 2. Phone Requirement & Format Check
            if (requiresPhone && phone.isEmpty()) {
                errors.add("Phone number is required for WhatsApp/SMS delivery channel");
            } else if (!phone.isEmpty() && !PHONE_PATTERN.matcher(phone).matches()) {
                errors.add("Invalid phone format (expected standard numbers or E.164, e.g. +1234567890)");
            }

            // 3. Email Requirement & Format Check
            if (requiresEmail && email.isEmpty()) {
                errors.add("Email address is required for Email delivery channel");
            } else if (!email.isEmpty() && !EMAIL_PATTERN.matcher(email).matches()) {
                errors.add("Invalid email format");
            }

            // 4. Duplicate Checks (In-Database and In-File)
            if (!email.isEmpty()) {
                if (guestRepository.existsByEventIdAndEmail(eventId, email)) {
                    isDuplicate = true;
                    errors.add("Guest with email '" + email + "' already exists in this event");
                } else if (seenEmailsInFile.contains(email.toLowerCase())) {
                    isDuplicate = true;
                    errors.add("Duplicate email '" + email + "' in file");
                }
            }

            if (!phone.isEmpty()) {
                if (guestRepository.existsByEventIdAndPhone(eventId, phone)) {
                    isDuplicate = true;
                    errors.add("Guest with phone '" + phone + "' already exists in this event");
                } else if (seenPhonesInFile.contains(phone)) {
                    isDuplicate = true;
                    errors.add("Duplicate phone '" + phone + "' in file");
                }
            }

            if (!fullName.isEmpty()) {
                if (guestRepository.existsByEventIdAndFullName(eventId, fullName)) {
                    isDuplicate = true;
                    errors.add("Guest with name '" + fullName + "' already exists in this event");
                } else if (seenNamesInFile.contains(fullName.toLowerCase())) {
                    isDuplicate = true;
                    errors.add("Duplicate name '" + fullName + "' in file");
                }
            }

            // Track in file sets
            if (!fullName.isEmpty()) seenNamesInFile.add(fullName.toLowerCase());
            if (!email.isEmpty()) seenEmailsInFile.add(email.toLowerCase());
            if (!phone.isEmpty()) seenPhonesInFile.add(phone);

            boolean isValid = errors.isEmpty();
            if (isValid) {
                validCount++;
            } else {
                invalidCount++;
                if (isDuplicate) duplicateCount++;
            }

            processedRows.add(GuestImportRowDto.builder()
                    .rowNumber(rowNum)
                    .fullName(fullName)
                    .phone(phone)
                    .email(email)
                    .valid(isValid)
                    .duplicate(isDuplicate)
                    .errors(errors)
                    .build());
        }

        return GuestImportPreviewDto.builder()
                .eventId(eventId)
                .fileName(fileName)
                .deliveryChannel(channel)
                .totalRows(processedRows.size())
                .validCount(validCount)
                .invalidCount(invalidCount)
                .duplicateCount(duplicateCount)
                .rows(processedRows)
                .build();
    }

    @Override
    @Transactional
    public GuestImportSummaryDto confirmImport(GuestImportConfirmRequestDto request) {
        if (!eventRepository.existsById(request.getEventId())) {
            throw new IllegalArgumentException("Event not found with ID: " + request.getEventId());
        }

        List<GuestImportRowDto> rowsToImport = request.getRowsToImport();
        if (rowsToImport == null || rowsToImport.isEmpty()) {
            return GuestImportSummaryDto.builder()
                    .eventId(request.getEventId())
                    .importedCount(0)
                    .skippedCount(0)
                    .importedGuests(List.of())
                    .build();
        }

        List<Guest> guestsToSave = new ArrayList<>();
        int skippedCount = 0;

        for (GuestImportRowDto row : rowsToImport) {
            if (!row.isValid()) {
                skippedCount++;
                continue;
            }

            Guest guest = Guest.builder()
                    .eventId(request.getEventId())
                    .fullName(row.getFullName())
                    .phone(row.getPhone())
                    .email(row.getEmail())
                    .build();

            guestsToSave.add(guest);
        }

        List<Guest> savedGuests = guestRepository.saveAll(guestsToSave);

        Event event = eventRepository.findById(request.getEventId()).orElse(null);
        if (event != null && event.getCurrentTemplateId() != null) {
            for (Guest saved : savedGuests) {
                invitationService.issueInheritedInvitation(saved.getEventId(), saved.getId(), AdmissionType.SINGLE);
            }
        }

        List<GuestResponseDto> dtos = savedGuests.stream()
                .map(g -> GuestResponseDto.builder()
                        .id(g.getId())
                        .eventId(g.getEventId())
                        .fullName(g.getFullName())
                        .phone(g.getPhone())
                        .email(g.getEmail())
                        .createdAt(g.getCreatedAt())
                        .updatedAt(g.getUpdatedAt())
                        .build())
                .toList();

        return GuestImportSummaryDto.builder()
                .eventId(request.getEventId())
                .importedCount(dtos.size())
                .skippedCount(skippedCount)
                .importedGuests(dtos)
                .build();
    }
}
