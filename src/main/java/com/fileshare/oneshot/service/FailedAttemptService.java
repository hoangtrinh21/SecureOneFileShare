package com.fileshare.oneshot.service;

import com.fileshare.oneshot.model.FailedAttempt;
import com.fileshare.oneshot.repository.FailedAttemptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class FailedAttemptService {

    @Autowired
    private FailedAttemptRepository failedAttemptRepository;

    public void recordFailedAttempt(String userEmail, String ipAddress) {
        Optional<FailedAttempt> failedAttemptOpt = failedAttemptRepository.findByUserEmailAndIpAddress(userEmail, ipAddress);
        
        FailedAttempt failedAttempt;
        if (failedAttemptOpt.isEmpty()) {
            failedAttempt = new FailedAttempt(userEmail, ipAddress);
        } else {
            failedAttempt = failedAttemptOpt.get();
            failedAttempt.incrementAttempt();
        }
        
        failedAttemptRepository.save(failedAttempt);
    }

    public boolean isLockedOut(String userEmail, String ipAddress) {
        Optional<FailedAttempt> failedAttemptOpt = failedAttemptRepository.findByUserEmailAndIpAddress(userEmail, ipAddress);
        return failedAttemptOpt.isPresent() && failedAttemptOpt.get().isLockedOut();
    }

    public long getRemainingLockoutSeconds(String userEmail, String ipAddress) {
        Optional<FailedAttempt> failedAttemptOpt = failedAttemptRepository.findByUserEmailAndIpAddress(userEmail, ipAddress);
        
        if (failedAttemptOpt.isPresent() && failedAttemptOpt.get().getLockoutUntil() != null) {
            FailedAttempt failedAttempt = failedAttemptOpt.get();
            Duration duration = Duration.between(LocalDateTime.now(), failedAttempt.getLockoutUntil());
            return Math.max(0, duration.getSeconds());
        }
        
        return 0;
    }

    public void resetFailedAttempts(String userEmail, String ipAddress) {
        Optional<FailedAttempt> failedAttemptOpt = failedAttemptRepository.findByUserEmailAndIpAddress(userEmail, ipAddress);
        
        if (failedAttemptOpt.isPresent()) {
            FailedAttempt failedAttempt = failedAttemptOpt.get();
            failedAttempt.reset();
            failedAttemptRepository.save(failedAttempt);
        }
    }

    @Scheduled(fixedRate = 3600000) // Run every hour
    public void cleanupExpiredLockouts() {
        failedAttemptRepository.cleanupExpiredLockouts();
    }
}
