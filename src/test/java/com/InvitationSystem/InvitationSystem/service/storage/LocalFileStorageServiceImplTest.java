package com.InvitationSystem.InvitationSystem.service.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalFileStorageServiceImplTest {

    @TempDir
    Path tempDir;

    private LocalFileStorageServiceImpl fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new LocalFileStorageServiceImpl(tempDir.toString());
    }

    @Test
    void storeFile_SuccessfulUpload_PngImage() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "design.png",
                "image/png",
                "fake image content".getBytes()
        );

        FileMetadata metadata = fileStorageService.storeFile(file, "WEDDING");

        assertNotNull(metadata);
        assertEquals("design.png", metadata.getOriginalFileName());
        assertEquals("image/png", metadata.getMimeType());
        assertTrue(metadata.getStoragePath().contains("WEDDING"));
        assertTrue(fileStorageService.exists(metadata.getStoragePath()));
    }

    @Test
    void storeFile_InvalidFileType_ThrowsException() {
        MockMultipartFile scriptFile = new MockMultipartFile(
                "file",
                "malicious.sh",
                "text/x-shellscript",
                "echo hello".getBytes()
        );

        assertThrows(IllegalArgumentException.class, () -> fileStorageService.storeFile(scriptFile, "GENERAL"));
    }

    @Test
    void storeFile_OversizedFile_ThrowsException() {
        byte[] largeBytes = new byte[11 * 1024 * 1024]; // 11MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "large.jpg",
                "image/jpeg",
                largeBytes
        );

        assertThrows(IllegalArgumentException.class, () -> fileStorageService.storeFile(largeFile, "GENERAL"));
    }

    @Test
    void storeFile_MissingFile_ThrowsException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.png",
                "image/png",
                new byte[0]
        );

        assertThrows(IllegalArgumentException.class, () -> fileStorageService.storeFile(emptyFile, "GENERAL"));
    }

    @Test
    void loadFile_RetrievalSuccess() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "card.jpg",
                "image/jpeg",
                "jpg data".getBytes()
        );

        FileMetadata metadata = fileStorageService.storeFile(file, "PARTY");
        byte[] loadedBytes = fileStorageService.loadFile(metadata.getStoragePath());

        assertNotNull(loadedBytes);
        assertArrayEquals("jpg data".getBytes(), loadedBytes);
    }

    @Test
    void deleteFile_DeletesFileSuccessfully() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "card.pdf",
                "application/pdf",
                "pdf data".getBytes()
        );

        FileMetadata metadata = fileStorageService.storeFile(file, "CONFERENCE");
        assertTrue(fileStorageService.exists(metadata.getStoragePath()));

        fileStorageService.deleteFile(metadata.getStoragePath());
        assertFalse(fileStorageService.exists(metadata.getStoragePath()));
    }

    @Test
    void pathTraversalAttempt_ThrowsSecurityException() {
        assertThrows(SecurityException.class, () -> fileStorageService.loadFile("../../../etc/passwd"));
    }
}
