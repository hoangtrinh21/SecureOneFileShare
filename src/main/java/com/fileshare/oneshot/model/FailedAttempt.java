package com.fileshare.oneshot.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class FailedAttempt {
    private String id;
    private String userEmail;
    private String ipAddress;
    private int attemptCount;
    private LocalDateTime lastAttemptTime;
    private LocalDateTime lockoutUntil;

    public FailedAttempt() {
        this.id = UUID.randomUUID().toString();
        this.attemptCount = 0;
    }

    public FailedAttempt(String userEmail, String ipAddress) {
        this.id = UUID.randomUUID().toString();
        this.userEmail = userEmail;
        this.ipAddress = ipAddress;
        this.attemptCount = 1;
        this.lastAttemptTime = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public LocalDateTime getLastAttemptTime() {
        return lastAttemptTime;
    }

    public void setLastAttemptTime(LocalDateTime lastAttemptTime) {
        this.lastAttemptTime = lastAttemptTime;
    }

    public LocalDateTime getLockoutUntil() {
        return lockoutUntil;
    }

    public void setLockoutUntil(LocalDateTime lockoutUntil) {
        this.lockoutUntil = lockoutUntil;
    }

    public void incrementAttempt() {
        this.attemptCount++;
        this.lastAttemptTime = LocalDateTime.now();

        if (this.attemptCount % 3 == 0) {
            int failedSets = this.attemptCount / 3;
            int minutes = (int) Math.pow(2, failedSets); // 2^1=2 phút cho 3 lần, 2^2=4 phút cho 6 lần,...
            this.lockoutUntil = LocalDateTime.now().plusMinutes(minutes);
        }
    }

    public boolean isLockedOut() {
        return lockoutUntil != null && LocalDateTime.now().isBefore(lockoutUntil);
    }

    public void reset() {
        this.attemptCount = 0;
        this.lockoutUntil = null;
    }
}
