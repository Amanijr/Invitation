package com.InvitationSystem.InvitationSystem.controller;

import com.InvitationSystem.InvitationSystem.Dto.templatesDto.TemplateRequestDto;
import com.InvitationSystem.InvitationSystem.Dto.templatesDto.TemplateResponseDto;
import com.InvitationSystem.InvitationSystem.entity.EventType;
import com.InvitationSystem.InvitationSystem.service.TemplateService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.WebDataBinder;

import java.beans.PropertyEditorSupport;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/templates")
@CrossOrigin(origins = "*")
@Tag(name = "Templates", description = "Invitation template management endpoints")
public class TemplateController {

    @Autowired
    private TemplateService templateService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TemplateResponseDto> createTemplate(@ModelAttribute TemplateRequestDto request) {
        try {
            TemplateResponseDto response = templateService.createTemplate(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{templateId}")
    public ResponseEntity<TemplateResponseDto> getTemplateById(@PathVariable UUID templateId) {
        try {
            return ResponseEntity.ok(templateService.getTemplateById(templateId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<TemplateResponseDto>> getAllTemplates() {
        return ResponseEntity.ok(templateService.getAllTemplates());
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
        try {
            return ResponseEntity.ok(templateService.updateTemplate(templateId, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{templateId}/deactivate")
    public ResponseEntity<TemplateResponseDto> deactivateTemplate(@PathVariable UUID templateId) {
        try {
            return ResponseEntity.ok(templateService.deactivateTemplate(templateId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID templateId) {
        templateService.deleteTemplate(templateId);
        return ResponseEntity.noContent().build();
    }


    // GET /api/v1/templates/active
    @GetMapping("/active")
    public ResponseEntity<List<TemplateResponseDto>> getAllActiveTemplates() {
        return ResponseEntity.ok(templateService.getAllActiveTemplates());
    }

    // GET /api/v1/templates/search?name=birthday
    @GetMapping("/search")
    public ResponseEntity<List<TemplateResponseDto>> searchTemplatesByName(@RequestParam String name) {
        return ResponseEntity.ok(templateService.searchTemplatesByName(name));
    }

    // GET /api/v1/templates/type/{eventType}/search?name=birthday
    @GetMapping("/type/{eventType}/search")
    public ResponseEntity<List<TemplateResponseDto>> searchTemplatesByEventTypeAndName(
            @PathVariable EventType eventType,
            @RequestParam String name) {
        return ResponseEntity.ok(templateService.searchTemplatesByEventTypeAndName(eventType, name));
    }


    // GET /api/v1/templates/type/{eventType}/count
    @GetMapping("/type/{eventType}/count")
    public ResponseEntity<Long> countTemplatesByEventType(@PathVariable EventType eventType) {
        return ResponseEntity.ok(templateService.countTemplatesByEventType(eventType));
    }

    // GET /api/v1/templates/type/{eventType}/count/active
    @GetMapping("/type/{eventType}/count/active")
    public ResponseEntity<Long> countActiveTemplatesByEventType(@PathVariable EventType eventType) {
        return ResponseEntity.ok(templateService.countActiveTemplatesByEventType(eventType));
    }



    // GET /api/v1/templates/event/{eventId}
    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<TemplateResponseDto>> getTemplatesByEventId(@PathVariable UUID eventId) {
        return ResponseEntity.ok(templateService.getTemplatesByEventId(eventId));
    }

    // GET /api/v1/templates/event/{eventId}/active
    @GetMapping("/event/{eventId}/active")
    public ResponseEntity<List<TemplateResponseDto>> getActiveTemplatesByEventId(@PathVariable UUID eventId) {
        return ResponseEntity.ok(templateService.getActiveTemplatesByEventId(eventId));
    }




    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(EventType.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(EventType.valueOf(text.toUpperCase()));
            }
        });
    }
}