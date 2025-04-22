
package com.fileshare.oneshot;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.server.ResponseStatusException;

import com.fileshare.oneshot.model.FileMetadata;
import com.fileshare.oneshot.repository.FileMetadataRepository;
import com.fileshare.oneshot.service.*;

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
    private GitSyncService gitSyncService;
    @Mock
    private OAuth2User oAuth2User;

    private FileService fileService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        fileService = new FileService(fileMetadataRepository, fileStorageService, 
                                    connectionCodeService, failedAttemptService, gitSyncService);
    }

    @Test
    void uploadWithoutAuth() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "test".getBytes());
        
        assertThrows(ResponseStatusException.class, () -> {
            fileService.processUpload(file, null);
        });
    }

    @Test
    void downloadWithoutAuth() {
        assertThrows(ResponseStatusException.class, () -> {
            fileService.validateDownloadToken("code");
        });
    }

    @Test
    void connectionCodeLengthTest() {
        // Test for 1 file
        when(fileMetadataRepository.countActiveFiles()).thenReturn(1L);
        when(connectionCodeService.generateConnectionCode(1L)).thenReturn("A");
        
        // Test for 2 files
        when(fileMetadataRepository.countActiveFiles()).thenReturn(2L);
        when(connectionCodeService.generateConnectionCode(2L)).thenReturn("AB");
        
        // Test for 60 files
        when(fileMetadataRepository.countActiveFiles()).thenReturn(60L);
        when(connectionCodeService.generateConnectionCode(60L)).thenReturn("ABC");
    }

    @Test 
    void connectionCodeExpirationTest() {
        FileMetadata metadata = new FileMetadata();
        String code = "TEST";
        
        // Test at 2 minutes
        metadata.setExpiryTime(LocalDateTime.now().plusMinutes(2));
        when(fileMetadataRepository.findByConnectionCode(code)).thenReturn(Optional.of(metadata));
        assertDoesNotThrow(() -> fileService.validateConnectionCode(code));
        
        // Test at 8 minutes
        metadata.setExpiryTime(LocalDateTime.now().minusMinutes(8));
        when(fileMetadataRepository.findByConnectionCode(code)).thenReturn(Optional.of(metadata));
        assertThrows(ResponseStatusException.class, () -> fileService.validateConnectionCode(code));
            
        // Test at 15 minutes
        metadata.setExpiryTime(LocalDateTime.now().minusMinutes(15));
        when(fileMetadataRepository.findByConnectionCode(code)).thenReturn(Optional.of(metadata));
        assertThrows(ResponseStatusException.class, () -> fileService.validateConnectionCode(code));
    }

    @Test
    void successfulDownloadTest() {
        String code = "TEST";
        FileMetadata metadata = new FileMetadata();
        metadata.setDownloaded(false);
        metadata.setExpiryTime(LocalDateTime.now().plusMinutes(5));
        
        when(fileMetadataRepository.findByConnectionCode(code)).thenReturn(Optional.of(metadata));
        assertDoesNotThrow(() -> fileService.validateConnectionCode(code));
    }

    @Test
    void preventSecondDownloadTest() {
        String code = "TEST";
        FileMetadata metadata = new FileMetadata();
        metadata.setDownloaded(true);
        metadata.setExpiryTime(LocalDateTime.now().plusMinutes(5));
        
        when(fileMetadataRepository.findByConnectionCode(code)).thenReturn(Optional.of(metadata));
        assertThrows(ResponseStatusException.class, () -> fileService.validateConnectionCode(code));
    }

    @Test
    void failedAttemptsLockoutTest() {
        String code = "WRONG";
        when(failedAttemptService.isLockedOut(anyString(), anyString())).thenReturn(true);
        
        // Test 6 failed attempts (5 minutes lockout)
        when(failedAttemptService.getRemainingLockoutSeconds(anyString(), anyString())).thenReturn(300L);
        assertThrows(ResponseStatusException.class, () -> fileService.validateConnectionCode(code));
        
        // Test 12 failed attempts (15 minutes lockout)
        when(failedAttemptService.getRemainingLockoutSeconds(anyString(), anyString())).thenReturn(900L);
        assertThrows(ResponseStatusException.class, () -> fileService.validateConnectionCode(code));
    }
}
