
package com.fileshare.oneshot;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import com.fileshare.oneshot.model.FailedAttempt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
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
    private ConnectionCodeService connectionCodeService;
    
    @InjectMocks
    private FileService fileService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void findByConnectionCodeTest() {
        // Arrange
        String connectionCode = "TEST";
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setConnectionCode(connectionCode);
        when(fileMetadataRepository.findByConnectionCode(connectionCode))
            .thenReturn(Optional.of(fileMetadata));
        
        // Act
        Optional<FileMetadata> result = fileService.findByConnectionCode(connectionCode);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(connectionCode, result.get().getConnectionCode());
    }

    @Test
    void connectionCodeLengthTest() {
        // Test for active files count
        when(fileMetadataRepository.countActiveFiles()).thenReturn(1L);
        assertEquals(1L, fileService.countActiveFiles());
        
        when(fileMetadataRepository.countActiveFiles()).thenReturn(39L);
        assertEquals(39L, fileService.countActiveFiles());
    }

    @Test
    void createDownloadTokenTest() {
        // Arrange
        FileMetadata fileMetadata = new FileMetadata();
        
        // Act
        String downloadToken = fileService.createDownloadToken(fileMetadata);
        
        // Assert
        assertNotNull(downloadToken);
        assertEquals(downloadToken, fileMetadata.getDownloadToken());
        assertNotNull(fileMetadata.getDownloadTokenExpiry());
        
        // Verify that downloadToken expiry is set to 3 minutes from now
        LocalDateTime now = LocalDateTime.now();
        assertTrue(fileMetadata.getDownloadTokenExpiry().isAfter(now));
        assertTrue(fileMetadata.getDownloadTokenExpiry().isBefore(now.plusMinutes(4)));
    }

    @Test
    void validateDownloadTokenTest() {
        // Arrange
        String validToken = "valid-token";
        String expiredToken = "expired-token";
        String nonExistentToken = "non-existent-token";
        
        FileMetadata validFileMetadata = new FileMetadata();
        validFileMetadata.setDownloadToken(validToken);
        validFileMetadata.setDownloadTokenExpiry(LocalDateTime.now().plusMinutes(2));
        
        FileMetadata expiredFileMetadata = new FileMetadata();
        expiredFileMetadata.setDownloadToken(expiredToken);
        expiredFileMetadata.setDownloadTokenExpiry(LocalDateTime.now().minusMinutes(1));
        
        when(fileMetadataRepository.findByDownloadToken(validToken))
            .thenReturn(Optional.of(validFileMetadata));
        when(fileMetadataRepository.findByDownloadToken(expiredToken))
            .thenReturn(Optional.of(expiredFileMetadata));
        when(fileMetadataRepository.findByDownloadToken(nonExistentToken))
            .thenReturn(Optional.empty());
        
        // Act & Assert
        assertNotNull(fileService.validateDownloadToken(validToken));
        assertNull(fileService.validateDownloadToken(expiredToken));
        assertNull(fileService.validateDownloadToken(nonExistentToken));
    }

    @Test
    void markAsDownloadedTest() {
        // Arrange
        String fileId = "file-id";
        String connectionCode = "connection-code";
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setId(fileId);
        fileMetadata.setFileName("test.txt");
        fileMetadata.setConnectionCode(connectionCode);
        fileMetadata.setDownloadToken("download-token");
        fileMetadata.setDownloadTokenExpiry(LocalDateTime.now().plusMinutes(2));
        
        when(fileMetadataRepository.findById(fileId)).thenReturn(Optional.of(fileMetadata));
        
        // Act
        fileService.markAsDownloaded(fileId);
        
        // Assert
        verify(connectionCodeService).releaseConnectionCode(connectionCode);
        verify(fileMetadataRepository).save(fileMetadata);
        
        // Check that file metadata was updated correctly
        assertTrue(fileMetadata.isDownloaded());
        assertNotNull(fileMetadata.getDownloadTime());
        assertNull(fileMetadata.getDownloadToken());
        assertNull(fileMetadata.getDownloadTokenExpiry());
    }

    /**
     * Kiểm tra logic thời gian khóa tài khoản khi có các lần đăng nhập thất bại.
     * Xác minh thời gian khóa tăng lên đúng dựa trên số lần thử không thành công.
     */
    @Test
    public void testLockoutDuration() {
        FailedAttempt attempt = new FailedAttempt("test@example.com", "127.0.0.1");

        // Simulate 6 failed attempts (2 sets of 3)
        for (int i = 0; i < 6; i++) {
            attempt.incrementAttempt();
        }

        LocalDateTime lockoutTime = attempt.getLockoutUntil();
        LocalDateTime expectedTime = attempt.getLastAttemptTime().plusMinutes(4); // 2nd set = 4 minutes
        assertEquals(0, ChronoUnit.SECONDS.between(expectedTime, lockoutTime));

        // Reset for testing 12 attempts
        attempt = new FailedAttempt("test@example.com", "127.0.0.1");

        // Simulate 12 failed attempts (4 sets of 3)
        for (int i = 0; i < 12; i++) {
            attempt.incrementAttempt();
        }

        lockoutTime = attempt.getLockoutUntil();
        expectedTime = attempt.getLastAttemptTime().plusMinutes(16); // 4th set = 16 minutes
        assertEquals(0, ChronoUnit.SECONDS.between(expectedTime, lockoutTime));
    }
}
