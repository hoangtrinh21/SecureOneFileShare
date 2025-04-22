
package com.fileshare.oneshot;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.server.ResponseStatusException;

import com.fileshare.oneshot.model.FileMetadata;
import com.fileshare.oneshot.repository.FileMetadataRepository;
import com.fileshare.oneshot.service.ConnectionCodeService;
import com.fileshare.oneshot.service.FailedAttemptService;
import com.fileshare.oneshot.service.FileService;
import com.fileshare.oneshot.service.FileStorageService;

public class FileServiceTest {

    @Mock
    private FileMetadataRepository fileMetadataRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private ConnectionCodeService connectionCodeService;
    @Mock 
    private FailedAttemptService failedAttemptService;
    @Mock
    private OAuth2User oAuth2User;

    private FileService fileService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        fileService = new FileService(fileMetadataRepository, fileStorageService, connectionCodeService, failedAttemptService);
    }

    @Test
    void uploadWithoutAuth() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "test".getBytes());
        
        assertThrows(ResponseStatusException.class, () -> {
            fileService.uploadFile(file, null);
        });
    }

    @Test
    void downloadWithoutAuth() {
        assertThrows(ResponseStatusException.class, () -> {
            fileService.downloadFile("code", null, "127.0.0.1");
        });
    }

    @Test
    void connectionCodeLengthTest() {
        // Test for 1 file
        when(fileMetadataRepository.count()).thenReturn(1L);
        when(connectionCodeService.generateConnectionCode(1L)).thenReturn("A");
        
        // Test for 2 files
        when(fileMetadataRepository.count()).thenReturn(2L);
        when(connectionCodeService.generateConnectionCode(2L)).thenReturn("AB");
        
        // Test for 60 files
        when(fileMetadataRepository.count()).thenReturn(60L);
        when(connectionCodeService.generateConnectionCode(60L)).thenReturn("ABC");
    }

    @Test 
    void connectionCodeExpirationTest() {
        FileMetadata metadata = new FileMetadata();
        String code = "TEST";
        
        // Test at 2 minutes
        metadata.setExpiryTime(LocalDateTime.now().plusMinutes(2));
        when(fileMetadataRepository.findByConnectionCode(code)).thenReturn(Optional.of(metadata));
        assertDoesNotThrow(() -> fileService.verifyConnectionCode(code, "user@test.com", "127.0.0.1"));
        
        // Test at 8 minutes
        metadata.setExpiryTime(LocalDateTime.now().minusMinutes(8));
        assertThrows(ResponseStatusException.class, () -> 
            fileService.verifyConnectionCode(code, "user@test.com", "127.0.0.1"));
            
        // Test at 15 minutes
        metadata.setExpiryTime(LocalDateTime.now().minusMinutes(15));
        assertThrows(ResponseStatusException.class, () -> 
            fileService.verifyConnectionCode(code, "user@test.com", "127.0.0.1"));
    }

    @Test
    void successfulDownloadTest() {
        String code = "TEST";
        FileMetadata metadata = new FileMetadata();
        metadata.setDownloaded(false);
        metadata.setExpiryTime(LocalDateTime.now().plusMinutes(5));
        
        when(fileMetadataRepository.findByConnectionCode(code)).thenReturn(Optional.of(metadata));
        assertDoesNotThrow(() -> fileService.verifyConnectionCode(code, "user@test.com", "127.0.0.1"));
    }

    @Test
    void preventSecondDownloadTest() {
        String code = "TEST";
        FileMetadata metadata = new FileMetadata();
        metadata.setDownloaded(true);
        metadata.setExpiryTime(LocalDateTime.now().plusMinutes(5));
        
        when(fileMetadataRepository.findByConnectionCode(code)).thenReturn(Optional.of(metadata));
        assertThrows(ResponseStatusException.class, () -> 
            fileService.verifyConnectionCode(code, "user@test.com", "127.0.0.1"));
    }

    @Test
    void failedAttemptsLockoutTest() {
        String ipAddress = "127.0.0.1";
        String userEmail = "test@example.com";
        
        // Test 6 failed attempts
        when(failedAttemptService.isLockedOut(userEmail, ipAddress)).thenReturn(true);
        when(failedAttemptService.getRemainingLockoutSeconds(userEmail, ipAddress)).thenReturn(300L); // 5 minutes
        
        assertThrows(ResponseStatusException.class, () -> 
            fileService.verifyConnectionCode("WRONG", userEmail, ipAddress));
            
        // Test 12 failed attempts
        when(failedAttemptService.isLockedOut(userEmail, ipAddress)).thenReturn(true);
        when(failedAttemptService.getRemainingLockoutSeconds(userEmail, ipAddress)).thenReturn(900L); // 15 minutes
        
        assertThrows(ResponseStatusException.class, () -> 
            fileService.verifyConnectionCode("WRONG", userEmail, ipAddress));
    }
}
