package com.InvitationSystem.InvitationSystem.service.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface FileStorageService {
    FileMetadata storeFile(MultipartFile file, String folder);
    byte[] loadFile(String storagePath);
    InputStream loadFileAsStream(String storagePath);
    void deleteFile(String storagePath);
    boolean exists(String storagePath);
}
