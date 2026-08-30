package com.InvitationSystem.InvitationSystem.service.storage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileMetadata {
    private String storagePath;
    private String originalFileName;
    private String mimeType;
    private long fileSize;
    private int width;
    private int height;
}
