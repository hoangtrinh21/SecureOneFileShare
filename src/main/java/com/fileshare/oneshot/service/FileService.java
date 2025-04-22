package com.fileshare.oneshot.service;

import com.fileshare.oneshot.model.FileMetadata;
import com.fileshare.oneshot.repository.FileMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileService {

    @Autowired
    private FileMetadataRepository fileMetadataRepository;
    
    @Autowired
    private ConnectionCodeService connectionCodeService;

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
        
        return fileMetadataRepository.save(fileMetadata);
    }

    public Optional<FileMetadata> findById(String id) {
        return fileMetadataRepository.findById(id);
    }

    public Optional<FileMetadata> findByConnectionCode(String connectionCode) {
        return fileMetadataRepository.findByConnectionCode(connectionCode);
    }

    public void deleteExpiredFiles() {
        List<FileMetadata> expiredFiles = fileMetadataRepository.findAllExpired();
        expiredFiles.forEach(file -> fileMetadataRepository.delete(file.getId()));
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
        }
    }
    
    public long countActiveFiles() {
        return fileMetadataRepository.countActiveFiles();
    }
}
