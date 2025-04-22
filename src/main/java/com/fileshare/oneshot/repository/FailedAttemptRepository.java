package com.fileshare.oneshot.repository;

import com.fileshare.oneshot.model.FailedAttempt;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class FailedAttemptRepository {
    
    // In-memory storage for failed attempts
    private final ConcurrentHashMap<String, FailedAttempt> failedAttemptsMap = new ConcurrentHashMap<>();
    
    public FailedAttempt save(FailedAttempt failedAttempt) {
        String key = generateKey(failedAttempt.getUserEmail(), failedAttempt.getIpAddress());
        failedAttemptsMap.put(key, failedAttempt);
        return failedAttempt;
    }
    
    public Optional<FailedAttempt> findByUserEmailAndIpAddress(String userEmail, String ipAddress) {
        String key = generateKey(userEmail, ipAddress);
        return Optional.ofNullable(failedAttemptsMap.get(key));
    }
    
    public void delete(String userEmail, String ipAddress) {
        String key = generateKey(userEmail, ipAddress);
        failedAttemptsMap.remove(key);
    }
    
    private String generateKey(String userEmail, String ipAddress) {
        // Use IP address if email is null (for unauthenticated users)
        return userEmail != null ? userEmail : "ip:" + ipAddress;
    }
    
    public void cleanupExpiredLockouts() {
        LocalDateTime now = LocalDateTime.now();
        failedAttemptsMap.entrySet().removeIf(entry -> {
            FailedAttempt attempt = entry.getValue();
            return attempt.getLockoutUntil() != null && 
                   attempt.getLockoutUntil().isBefore(now) && 
                   attempt.getLastAttemptTime().plusDays(1).isBefore(now);
        });
    }
}
