package com.InvitationSystem.InvitationSystem.service.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class LocalFileStorageServiceImpl implements FileStorageService {

    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "application/pdf"
    );

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            ".png", ".jpg", ".jpeg", ".pdf"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    private final Path rootLocation;

    public LocalFileStorageServiceImpl(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directory at " + uploadDir, e);
        }
    }

    @Override
    public FileMetadata storeFile(MultipartFile file, String folder) {
        validateFile(file);

        String originalFileName = sanitizeFileName(file.getOriginalFilename());
        String extension = getFileExtension(originalFileName);
        String uniqueFileName = UUID.randomUUID().toString() + extension;

        Path targetFolder = rootLocation.resolve(sanitizeFileName(folder)).normalize();
        
        // Prevent path traversal
        if (!targetFolder.startsWith(rootLocation)) {
            throw new SecurityException("Path traversal attempt detected");
        }

        try {
            Files.createDirectories(targetFolder);
            Path destinationFile = targetFolder.resolve(uniqueFileName).normalize();

            if (!destinationFile.startsWith(targetFolder)) {
                throw new SecurityException("Path traversal attempt detected in filename");
            }

            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

            int width = 1920;
            int height = 1080;

            // Extract image dimensions if PNG or JPEG
            if (isImage(file.getContentType(), extension)) {
                try (InputStream is = file.getInputStream()) {
                    BufferedImage image = ImageIO.read(is);
                    if (image != null) {
                        width = image.getWidth();
                        height = image.getHeight();
                    }
                } catch (Exception e) {
                    // Fallback to default dimensions if image parsing fails
                }
            }

            String relativePath = rootLocation.relativize(destinationFile).toString();

            return FileMetadata.builder()
                    .storagePath(relativePath)
                    .originalFileName(file.getOriginalFilename())
                    .mimeType(file.getContentType())
                    .fileSize(file.getSize())
                    .width(width)
                    .height(height)
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file " + originalFileName, e);
        }
    }

    @Override
    public byte[] loadFile(String storagePath) {
        Path file = getAbsolutePath(storagePath);
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new RuntimeException("Could not read file: " + storagePath, e);
        }
    }

    @Override
    public InputStream loadFileAsStream(String storagePath) {
        Path file = getAbsolutePath(storagePath);
        try {
            return Files.newInputStream(file);
        } catch (IOException e) {
            throw new RuntimeException("Could not read file stream: " + storagePath, e);
        }
    }

    @Override
    public void deleteFile(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return;
        }
        try {
            Path file = getAbsolutePath(storagePath);
            Files.deleteIfExists(file);
        } catch (IOException e) {
            System.err.println("Failed to delete file " + storagePath + ": " + e.getMessage());
        }
    }

    @Override
    public boolean exists(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return false;
        }
        try {
            Path file = getAbsolutePath(storagePath);
            return Files.exists(file);
        } catch (Exception e) {
            return false;
        }
    }

    private Path getAbsolutePath(String relativePath) {
        Path resolved = rootLocation.resolve(relativePath).normalize();
        if (!resolved.startsWith(rootLocation)) {
            throw new SecurityException("Path traversal attack detected: " + relativePath);
        }
        if (!Files.exists(resolved)) {
            throw new IllegalArgumentException("File not found: " + relativePath);
        }
        return resolved;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 10MB");
        }

        String contentType = file.getContentType();
        String originalFileName = file.getOriginalFilename();
        String extension = getFileExtension(originalFileName);

        boolean validMime = contentType != null && ALLOWED_MIME_TYPES.contains(contentType.toLowerCase());
        boolean validExt = ALLOWED_EXTENSIONS.contains(extension.toLowerCase());

        if (!validMime && !validExt) {
            throw new IllegalArgumentException("Invalid file type. Only PNG, JPEG, and PDF files are allowed.");
        }
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "unnamed_file";
        }
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    private boolean isImage(String mimeType, String extension) {
        if (mimeType != null && (mimeType.contains("png") || mimeType.contains("jpeg") || mimeType.contains("jpg"))) {
            return true;
        }
        String ext = extension.toLowerCase();
        return ext.endsWith(".png") || ext.endsWith(".jpg") || ext.endsWith(".jpeg");
    }
}
