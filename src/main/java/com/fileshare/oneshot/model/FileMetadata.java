package com.fileshare.oneshot.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class FileMetadata {
    private String id;
    private String fileName;
    private long fileSize;
    private String contentType;
    private String uploaderEmail;
    private String uploaderName;
    private String connectionCode;
    private LocalDateTime uploadTime;
    private LocalDateTime expiryTime;
    private String downloadToken;
    private LocalDateTime downloadTokenExpiry;
    private boolean downloaded;
    private LocalDateTime downloadTime;

    public FileMetadata() {
        this.id = UUID.randomUUID().toString();
        this.uploadTime = LocalDateTime.now();
        this.expiryTime = uploadTime.plusMinutes(10);
        this.downloaded = false;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getUploaderEmail() {
        return uploaderEmail;
    }

    public void setUploaderEmail(String uploaderEmail) {
        this.uploaderEmail = uploaderEmail;
    }

    public String getUploaderName() {
        return uploaderName;
    }

    public void setUploaderName(String uploaderName) {
        this.uploaderName = uploaderName;
    }

    public String getConnectionCode() {
        return connectionCode;
    }

    public void setConnectionCode(String connectionCode) {
        this.connectionCode = connectionCode;
    }

    public LocalDateTime getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }

    public LocalDateTime getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(LocalDateTime expiryTime) {
        this.expiryTime = expiryTime;
    }

    public String getDownloadToken() {
        return downloadToken;
    }

    public void setDownloadToken(String downloadToken) {
        this.downloadToken = downloadToken;
    }

    public LocalDateTime getDownloadTokenExpiry() {
        return downloadTokenExpiry;
    }

    public void setDownloadTokenExpiry(LocalDateTime downloadTokenExpiry) {
        this.downloadTokenExpiry = downloadTokenExpiry;
    }

    public boolean isDownloaded() {
        return downloaded;
    }

    public void setDownloaded(boolean downloaded) {
        this.downloaded = downloaded;
    }

    public LocalDateTime getDownloadTime() {
        return downloadTime;
    }

    public void setDownloadTime(LocalDateTime downloadTime) {
        this.downloadTime = downloadTime;
    }
}
