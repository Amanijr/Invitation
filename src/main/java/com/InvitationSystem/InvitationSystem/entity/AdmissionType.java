package com.InvitationSystem.InvitationSystem.entity;

@io.swagger.v3.oas.annotations.media.Schema(description = "Invitation admission entitlement")
public enum AdmissionType {
    SINGLE(1),
    DOUBLE(2);

    private final int admissionLimit;

    AdmissionType(int admissionLimit) {
        this.admissionLimit = admissionLimit;
    }

    public int getAdmissionLimit() {
        return admissionLimit;
    }

    public static AdmissionType fromNullable(AdmissionType value) {
        return value == null ? SINGLE : value;
    }
}
