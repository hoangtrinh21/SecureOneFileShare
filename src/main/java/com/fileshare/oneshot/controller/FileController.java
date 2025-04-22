package com.fileshare.oneshot.controller;

import com.fileshare.oneshot.model.FileMetadata;
import com.fileshare.oneshot.service.ConnectionCodeService;
import com.fileshare.oneshot.service.FailedAttemptService;
import com.fileshare.oneshot.service.FileService;
import com.fileshare.oneshot.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class FileController {

    @Autowired
    private FileService fileService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ConnectionCodeService connectionCodeService;

    @Autowired
    private FailedAttemptService failedAttemptService;

    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal OAuth2User principal) {

        // Check if file is empty
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please select a file to upload");
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File size exceeds the limit of 2MB");
        }

        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name");

        try {
            // Generate connection code
            String connectionCode = connectionCodeService.generateConnectionCode(fileService.countActiveFiles());
            
            // Store file metadata and actual file
            FileMetadata fileMetadata = fileService.saveFileMetadata(
                    file.getOriginalFilename(),
                    file.getSize(),
                    file.getContentType(),
                    email,
                    name,
                    connectionCode
            );
            
            fileStorageService.storeFile(file, fileMetadata.getId());

            Map<String, String> response = new HashMap<>();
            response.put("connectionCode", connectionCode);
            response.put("fileName", file.getOriginalFilename());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload file: " + e.getMessage());
        }
    }

    @PostMapping("/download")
    public ResponseEntity<Map<String, Object>> verifyConnectionCode(
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal OAuth2User principal,
            HttpServletRequest request) {
        
        String connectionCode = payload.get("connectionCode");
        if (connectionCode == null || connectionCode.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Connection code is required");
        }
        
        String userEmail = principal != null ? principal.getAttribute("email") : null;
        String ipAddress = request.getRemoteAddr();
        
        // Check if user is locked out
        if (failedAttemptService.isLockedOut(userEmail, ipAddress)) {
            long lockoutTimeSeconds = failedAttemptService.getRemainingLockoutSeconds(userEmail, ipAddress);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Too many failed attempts");
            response.put("lockedOutFor", lockoutTimeSeconds);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
        }
        
        // Try to find file by connection code
        Optional<FileMetadata> fileMetadataOpt = fileService.findByConnectionCode(connectionCode);
        
        if (fileMetadataOpt.isEmpty()) {
            // Record failed attempt
            failedAttemptService.recordFailedAttempt(userEmail, ipAddress);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid connection code");
        }
        
        FileMetadata fileMetadata = fileMetadataOpt.get();
        
        // Check if code is expired
        if (LocalDateTime.now().isAfter(fileMetadata.getExpiryTime())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Connection code has expired");
        }
        
        // Check if file is already downloaded
        if (fileMetadata.isDownloaded()) {
            throw new ResponseStatusException(HttpStatus.GONE, "File has already been downloaded");
        }
        
        // Check if the user trying to download is the same as the uploader
        if (userEmail != null && userEmail.equals(fileMetadata.getUploaderEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot download your own file");
        }
        
        // Create a temporary download token valid for 3 minutes
        String downloadToken = fileService.createDownloadToken(fileMetadata);
        
        // Reset failed attempts
        failedAttemptService.resetFailedAttempts(userEmail, ipAddress);
        
        Map<String, Object> response = new HashMap<>();
        response.put("fileName", fileMetadata.getFileName());
        response.put("fileSize", fileMetadata.getFileSize());
        response.put("contentType", fileMetadata.getContentType());
        response.put("downloadToken", downloadToken);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(
            @RequestParam("token") String downloadToken,
            HttpServletResponse response) {
            
        // Validate download token
        FileMetadata fileMetadata = fileService.validateDownloadToken(downloadToken);
        if (fileMetadata == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid or expired download token");
        }
        
        try {
            // Get file as resource
            Resource resource = fileStorageService.loadFileAsResource(fileMetadata.getId());
            
            // Mark file as downloaded
            fileService.markAsDownloaded(fileMetadata.getId());
            
            String contentType = fileMetadata.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileMetadata.getFileName() + "\"")
                    .body(resource);
                    
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error downloading file");
        }
    }
}
