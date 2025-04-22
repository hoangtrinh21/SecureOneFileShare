package com.fileshare.oneshot.scheduler;

import com.fileshare.oneshot.model.FileMetadata;
import com.fileshare.oneshot.repository.FileMetadataRepository;
import com.fileshare.oneshot.service.FileService;
import com.fileshare.oneshot.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FileCleanupScheduler {

    @Autowired
    private FileService fileService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    /**
     * Cleanup expired files every 5 minutes
     */
    @Scheduled(fixedRate = 300000)
    public void cleanupExpiredFiles() {
        List<FileMetadata> expiredFiles = fileMetadataRepository.findAllExpired();
        
        for (FileMetadata file : expiredFiles) {
            // Delete the physical file
            fileStorageService.deleteFile(file.getId());
            
            // Delete the metadata
            fileMetadataRepository.delete(file.getId());
        }
        
        System.out.println("Cleaned up " + expiredFiles.size() + " expired files");
    }
}
