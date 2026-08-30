package com.InvitationSystem.InvitationSystem.controller;

import com.InvitationSystem.InvitationSystem.Dto.templatesDto.TemplateFieldConfigDto;
import com.InvitationSystem.InvitationSystem.Dto.templatesDto.TemplatePreviewRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.templatesDto.TemplatePreviewResponseDto;
import com.InvitationSystem.InvitationSystem.Dto.templatesDto.TemplateRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.templatesDto.TemplateResponseDto;
import com.InvitationSystem.InvitationSystem.entity.EventType;
import com.InvitationSystem.InvitationSystem.service.TemplateFieldConfigService;
import com.InvitationSystem.InvitationSystem.service.TemplateService;
import com.InvitationSystem.InvitationSystem.util.ImageCardGeneratorService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.beans.PropertyEditorSupport;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/templates")
@Tag(name = "Templates", description = "Invitation template management endpoints")
public class TemplateController {

    @Autowired
    private TemplateService templateService;

    @Autowired
    private TemplateFieldConfigService fieldConfigService;

    @Autowired
    private ImageCardGeneratorService imageCardGeneratorService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TemplateResponseDto> createTemplate(@ModelAttribute TemplateRequestDto request) {
        TemplateResponseDto response = templateService.createTemplate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{templateId}")
    public ResponseEntity<TemplateResponseDto> getTemplateById(@PathVariable UUID templateId) {
        return ResponseEntity.ok(templateService.getTemplateById(templateId));
    }

    @GetMapping("/{templateId}/file")
    public ResponseEntity<byte[]> getTemplateFile(@PathVariable UUID templateId) {
        TemplateResponseDto template = templateService.getTemplateById(templateId);
        if (template.getStoragePath() == null || template.getStoragePath().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        byte[] data;
        try {
            data = templateService.loadTemplateFile(templateId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }

        String contentType = template.getMimeType() != null ? template.getMimeType() : "application/octet-stream";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + asciiFileName(template.getOriginalFileName()) + "\"")
                .body(data);
    }

    /** HTTP headers must be ISO-8859-1; Unicode filenames make Tomcat return 400. */
    static String asciiFileName(String original) {
        if (original == null || original.isBlank()) {
            return "card";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < original.length(); i++) {
            char c = original.charAt(i);
            if (c == '"' || c == '\\' || c == '\r' || c == '\n') {
                continue;
            }
            if (c >= 0x20 && c <= 0x7E) {
                out.append(c);
            }
        }
        String cleaned = out.toString().trim();
        return cleaned.isEmpty() ? "card" : cleaned;
    }

    @GetMapping
    public ResponseEntity<List<TemplateResponseDto>> getAllTemplates() {
        return ResponseEntity.ok(templateService.getAllTemplates());
    }

    @GetMapping("/active")
    public ResponseEntity<List<TemplateResponseDto>> getAllActiveTemplates() {
        return ResponseEntity.ok(templateService.getAllActiveTemplates());
    }

    @GetMapping("/type/{eventType}")
    public ResponseEntity<List<TemplateResponseDto>> getTemplatesByEventType(@PathVariable EventType eventType) {
        return ResponseEntity.ok(templateService.getTemplatesByEventType(eventType));
    }

    @GetMapping("/type/{eventType}/active")
    public ResponseEntity<List<TemplateResponseDto>> getActiveTemplatesByEventType(@PathVariable EventType eventType) {
        return ResponseEntity.ok(templateService.getActiveTemplatesByEventType(eventType));
    }

    @PutMapping(value = "/{templateId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TemplateResponseDto> updateTemplate(
            @PathVariable UUID templateId,
            @ModelAttribute TemplateRequestDto request) {
        return ResponseEntity.ok(templateService.updateTemplate(templateId, request));
    }

    @PatchMapping("/{templateId}/activate")
    public ResponseEntity<TemplateResponseDto> activateTemplate(@PathVariable UUID templateId) {
        return ResponseEntity.ok(templateService.activateTemplate(templateId));
    }

    @PatchMapping("/{templateId}/deactivate")
    public ResponseEntity<TemplateResponseDto> deactivateTemplate(@PathVariable UUID templateId) {
        return ResponseEntity.ok(templateService.deactivateTemplate(templateId));
    }

    @DeleteMapping
    public ResponseEntity<java.util.Map<String, Integer>> deleteAllTemplates() {
        int deletedCount = templateService.deleteAllTemplates();
        return ResponseEntity.ok(java.util.Map.of("deletedCount", deletedCount));
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID templateId) {
        templateService.deleteTemplate(templateId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<TemplateResponseDto>> searchTemplatesByName(@RequestParam String name) {
        return ResponseEntity.ok(templateService.searchTemplatesByName(name));
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<TemplateResponseDto>> getTemplatesByEventId(@PathVariable UUID eventId) {
        return ResponseEntity.ok(templateService.getTemplatesByEventId(eventId));
    }

    @GetMapping("/event/{eventId}/active")
    public ResponseEntity<List<TemplateResponseDto>> getActiveTemplatesByEventId(@PathVariable UUID eventId) {
        return ResponseEntity.ok(templateService.getActiveTemplatesByEventId(eventId));
    }

    // ==========================================
    // TEMPLATE FIELD POSITIONING CONFIG ENDPOINTS
    // ==========================================

    @GetMapping("/{templateId}/fields")
    public ResponseEntity<List<TemplateFieldConfigDto>> getFieldConfigs(@PathVariable UUID templateId) {
        return ResponseEntity.ok(fieldConfigService.getFieldConfigsByTemplateId(templateId));
    }

    @PostMapping("/{templateId}/fields")
    public ResponseEntity<List<TemplateFieldConfigDto>> saveFieldConfigs(
            @PathVariable UUID templateId,
            @RequestBody List<TemplateFieldConfigDto> configs) {
        List<TemplateFieldConfigDto> saved = fieldConfigService.saveFieldConfigs(templateId, configs);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{templateId}/fields/{fieldId}")
    public ResponseEntity<TemplateFieldConfigDto> updateFieldConfig(
            @PathVariable UUID templateId,
            @PathVariable UUID fieldId,
            @Valid @RequestBody TemplateFieldConfigDto config) {
        return ResponseEntity.ok(fieldConfigService.updateFieldConfig(templateId, fieldId, config));
    }

    @DeleteMapping("/{templateId}/fields/{fieldId}")
    public ResponseEntity<Void> deleteFieldConfig(
            @PathVariable UUID templateId,
            @PathVariable UUID fieldId) {
        fieldConfigService.deleteFieldConfig(templateId, fieldId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{templateId}/fields")
    public ResponseEntity<Void> deleteAllFieldConfigs(@PathVariable UUID templateId) {
        fieldConfigService.deleteAllFieldConfigsByTemplateId(templateId);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // PREVIEW RENDERING ENDPOINTS
    // ==========================================

    @PostMapping("/{templateId}/preview")
    public ResponseEntity<TemplatePreviewResponseDto> generatePreview(
            @PathVariable UUID templateId,
            @RequestBody(required = false) TemplatePreviewRequestDto request) {
        TemplateResponseDto template = templateService.getTemplateById(templateId);
        byte[] bgBytes = null;
        try {
            bgBytes = templateService.loadTemplateFile(templateId);
        } catch (Exception e) {
            // Handled internally by renderer fallback
        }

        List<TemplateFieldConfigDto> configs;
        if (request != null && request.getFieldConfigs() != null && !request.getFieldConfigs().isEmpty()) {
            configs = request.getFieldConfigs();
        } else {
            configs = fieldConfigService.getFieldConfigsByTemplateId(templateId);
        }

        Map<String, String> dataMap = request != null ? request.toSampleDataMap() : new TemplatePreviewRequestDto().toSampleDataMap();

        String base64Image = imageCardGeneratorService.renderCardImageBase64(bgBytes, configs, dataMap);

        TemplatePreviewResponseDto response = TemplatePreviewResponseDto.builder()
                .templateId(templateId)
                .base64Image("data:image/png;base64," + base64Image)
                .mimeType("image/png")
                .width(template.getWidth())
                .height(template.getHeight())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{templateId}/preview/image")
    public ResponseEntity<byte[]> generatePreviewImage(@PathVariable UUID templateId) {
        TemplateResponseDto template = templateService.getTemplateById(templateId);
        byte[] bgBytes = null;
        try {
            bgBytes = templateService.loadTemplateFile(templateId);
        } catch (Exception e) {
            // Handled by renderer fallback
        }

        List<TemplateFieldConfigDto> configs = fieldConfigService.getFieldConfigsByTemplateId(templateId);
        Map<String, String> dataMap = new TemplatePreviewRequestDto().toSampleDataMap();

        byte[] renderedPng = imageCardGeneratorService.renderCardImage(bgBytes, configs, dataMap);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"preview-" + templateId + ".png\"")
                .body(renderedPng);
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(EventType.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                if (text != null && !text.isBlank()) {
                    setValue(EventType.valueOf(text.toUpperCase()));
                }
            }
        });
    }
}