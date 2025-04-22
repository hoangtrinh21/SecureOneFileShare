package com.fileshare.oneshot.repository;

import com.fileshare.oneshot.model.FileMetadata;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class FileMetadataRepository {
    
    // In-memory storage for file metadata
    private final ConcurrentHashMap<String, FileMetadata> fileMetadataMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, FileMetadata> connectionCodeMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, FileMetadata> downloadTokenMap = new ConcurrentHashMap<>();
    
    public FileMetadata save(FileMetadata fileMetadata) {
        fileMetadataMap.put(fileMetadata.getId(), fileMetadata);
        if (fileMetadata.getConnectionCode() != null) {
            connectionCodeMap.put(fileMetadata.getConnectionCode(), fileMetadata);
        }
        if (fileMetadata.getDownloadToken() != null) {
            downloadTokenMap.put(fileMetadata.getDownloadToken(), fileMetadata);
        }
        return fileMetadata;
    }
    
    public Optional<FileMetadata> findById(String id) {
        return Optional.ofNullable(fileMetadataMap.get(id));
    }
    
    public Optional<FileMetadata> findByConnectionCode(String connectionCode) {
        return Optional.ofNullable(connectionCodeMap.get(connectionCode));
    }
    
    public Optional<FileMetadata> findByDownloadToken(String downloadToken) {
        return Optional.ofNullable(downloadTokenMap.get(downloadToken));
    }
    
    public void delete(String id) {
        FileMetadata metadata = fileMetadataMap.remove(id);
        if (metadata != null) {
            if (metadata.getConnectionCode() != null) {
                connectionCodeMap.remove(metadata.getConnectionCode());
            }
            if (metadata.getDownloadToken() != null) {
                downloadTokenMap.remove(metadata.getDownloadToken());
            }
        }
    }
    
    public List<FileMetadata> findAllWaitingDownload() {
        return fileMetadataMap.values().stream()
                .filter(fm -> !fm.isDownloaded() && LocalDateTime.now().isBefore(fm.getExpiryTime()))
                .collect(Collectors.toList());
    }
    
    public List<FileMetadata> findAllExpired() {
        return fileMetadataMap.values().stream()
                .filter(fm -> LocalDateTime.now().isAfter(fm.getExpiryTime()) || 
                        (fm.isDownloaded() && fm.getDownloadTime().plusHours(1).isBefore(LocalDateTime.now())))
                .collect(Collectors.toList());
    }
    
    public void removeDownloadToken(String token) {
        downloadTokenMap.remove(token);
    }
    
    public long countActiveFiles() {
        return fileMetadataMap.values().stream()
                .filter(fm -> !fm.isDownloaded() && LocalDateTime.now().isBefore(fm.getExpiryTime()))
                .count();
    }
}
