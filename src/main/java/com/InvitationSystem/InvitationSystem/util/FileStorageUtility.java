package com.InvitationSystem.InvitationSystem.util;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageUtility {

    public String saveFile(MultipartFile file, String eventTypeFolder) {
        try {
            String uploadDir = "uploads/templates/" + eventTypeFolder.toLowerCase() + "/";
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

            Path path = Paths.get(uploadDir + fileName);
            Files.createDirectories(path.getParent());
            Files.write(path, file.getBytes());

            return path.toString();

        } catch (IOException e) {
            throw new RuntimeException("File upload failed", e);
        }
    }
}
