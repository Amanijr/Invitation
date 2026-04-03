package com.InvitationSystem.InvitationSystem.controller;

import com.InvitationSystem.InvitationSystem.Dto.bulkUploadDto.BulkUploadRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.bulkUploadDto.BulkUploadResponseDto;
import com.InvitationSystem.InvitationSystem.Dto.bulkUploadDto.BulkUploadSessionDto;
import com.InvitationSystem.InvitationSystem.service.BulkUploadService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bulk-uploads")
@CrossOrigin(origins = "*")
@Tag(name = "Bulk Uploads", description = "Bulk invitation upload and processing endpoints")
public class BulkUploadController {

    @Autowired
    private BulkUploadService bulkUploadService;

    @PostMapping("/process")
    public ResponseEntity<BulkUploadResponseDto> processBulkUpload(@RequestBody BulkUploadRequestDto request, @RequestParam UUID uploadedBy) {
        BulkUploadResponseDto response = bulkUploadService.processBulkUpload(request, uploadedBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/upload")
    public ResponseEntity<BulkUploadResponseDto> uploadBulkFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("eventId") UUID eventId,
            @RequestParam(value = "templateId", required = false) UUID templateId,
            @RequestParam("uploadedBy") UUID uploadedBy) {
        try {
            // Create BulkUploadRequestDto from the multipart file
            BulkUploadRequestDto request = new BulkUploadRequestDto();
            request.setEventId(eventId);
            request.setTemplateId(templateId);
            request.setFileName(file.getOriginalFilename());
            request.setFileType(getFileType(file.getOriginalFilename()));
            request.setFileContent(file.getBytes());

            BulkUploadResponseDto response = bulkUploadService.processBulkUpload(request, uploadedBy);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<BulkUploadSessionDto> getUploadSession(@PathVariable UUID sessionId) {
        BulkUploadSessionDto response = bulkUploadService.getUploadSession(sessionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<BulkUploadSessionDto>> getUploadSessionsByEvent(@PathVariable UUID eventId) {
        List<BulkUploadSessionDto> sessions = bulkUploadService.getUploadSessionsByEvent(eventId);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/user/{uploadedBy}")
    public ResponseEntity<List<BulkUploadSessionDto>> getUploadSessionsByUser(@PathVariable UUID uploadedBy) {
        List<BulkUploadSessionDto> sessions = bulkUploadService.getUploadSessionsByUser(uploadedBy);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping
    public ResponseEntity<List<BulkUploadSessionDto>> getAllUploadSessions() {
        List<BulkUploadSessionDto> sessions = bulkUploadService.getAllUploadSessions();
        return ResponseEntity.ok(sessions);
    }

    private String getFileType(String fileName) {
        if (fileName != null) {
            if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
                return "EXCEL";
            } else if (fileName.endsWith(".pdf")) {
                return "PDF";
            }
        }
        throw new IllegalArgumentException("Unsupported file type. Please use .xlsx or .xls files.");
    }
}
