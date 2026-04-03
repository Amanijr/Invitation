package com.InvitationSystem.InvitationSystem.entity;

@io.swagger.v3.oas.annotations.media.Schema(description = "Bulk upload processing statuses")
public enum UploadStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}
