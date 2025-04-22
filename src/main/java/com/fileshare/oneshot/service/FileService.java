package com.fileshare.oneshot.service;

import com.fileshare.oneshot.model.FileMetadata;
import com.fileshare.oneshot.repository.FileMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    @Autowired
    private FileMetadataRepository fileMetadataRepository;
    
    @Autowired
    private ConnectionCodeService connectionCodeService;
    
    @Autowired
    private GitSyncService gitSyncService;

    public FileMetadata saveFileMetadata(String fileName, long fileSize, String contentType, 
                                         String uploaderEmail, String uploaderName, String connectionCode) {
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setFileName(fileName);
        fileMetadata.setFileSize(fileSize);
        fileMetadata.setContentType(contentType);
        fileMetadata.setUploaderEmail(uploaderEmail);
        fileMetadata.setUploaderName(uploaderName);
        fileMetadata.setConnectionCode(connectionCode);
        fileMetadata.setUploadTime(LocalDateTime.now());
        fileMetadata.setExpiryTime(LocalDateTime.now().plusMinutes(10));
        fileMetadata.setDownloaded(false);
        
        FileMetadata savedMetadata = fileMetadataRepository.save(fileMetadata);
        
        // Tự động commit và đồng bộ khi file được tải lên
        gitSyncService.syncChanges("Upload file: " + fileName + " with code: " + connectionCode);
        
        return savedMetadata;
    }

    public Optional<FileMetadata> findById(String id) {
        return fileMetadataRepository.findById(id);
    }

    public Optional<FileMetadata> findByConnectionCode(String connectionCode) {
        return fileMetadataRepository.findByConnectionCode(connectionCode);
    }

    public void deleteExpiredFiles() {
        List<FileMetadata> expiredFiles = fileMetadataRepository.findAllExpired();
        
        if (!expiredFiles.isEmpty()) {
            expiredFiles.forEach(file -> fileMetadataRepository.delete(file.getId()));
            
            // Tự động commit và đồng bộ khi có file hết hạn bị xóa
            int count = expiredFiles.size();
            gitSyncService.syncChanges("Deleted " + count + " expired file(s)");
            
            logger.info("Deleted {} expired files and synced changes", count);
        }
    }

    public String createDownloadToken(FileMetadata fileMetadata) {
        String downloadToken = UUID.randomUUID().toString();
        fileMetadata.setDownloadToken(downloadToken);
        fileMetadata.setDownloadTokenExpiry(LocalDateTime.now().plusMinutes(3));
        fileMetadataRepository.save(fileMetadata);
        return downloadToken;
    }

    public FileMetadata validateDownloadToken(String downloadToken) {
        Optional<FileMetadata> fileMetadataOpt = fileMetadataRepository.findByDownloadToken(downloadToken);
        
        if (fileMetadataOpt.isEmpty()) {
            return null;
        }
        
        FileMetadata fileMetadata = fileMetadataOpt.get();
        
        // Check if token is expired
        if (LocalDateTime.now().isAfter(fileMetadata.getDownloadTokenExpiry())) {
            fileMetadata.setDownloadToken(null);
            fileMetadata.setDownloadTokenExpiry(null);
            fileMetadataRepository.save(fileMetadata);
            return null;
        }
        
        return fileMetadata;
    }

    public void markAsDownloaded(String fileId) {
        Optional<FileMetadata> fileMetadataOpt = fileMetadataRepository.findById(fileId);
        if (fileMetadataOpt.isPresent()) {
            FileMetadata fileMetadata = fileMetadataOpt.get();
            fileMetadata.setDownloaded(true);
            fileMetadata.setDownloadTime(LocalDateTime.now());
            
            // Release the connection code for reuse
            connectionCodeService.releaseConnectionCode(fileMetadata.getConnectionCode());
            
            // Clear download token
            fileMetadata.setDownloadToken(null);
            fileMetadata.setDownloadTokenExpiry(null);
            
            fileMetadataRepository.save(fileMetadata);
            
            // Tự động commit và đồng bộ khi file được tải xuống
            gitSyncService.syncChanges("Downloaded file: " + fileMetadata.getFileName() + " with code: " + fileMetadata.getConnectionCode());
            
            logger.info("File [{}] has been downloaded and changes synced", fileMetadata.getFileName());
        }
    }
    
    public long countActiveFiles() {
        return fileMetadataRepository.countActiveFiles();
    }
}
