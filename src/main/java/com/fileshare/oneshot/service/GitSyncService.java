package com.fileshare.oneshot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;

@Service
public class GitSyncService {
    
    private static final Logger logger = LoggerFactory.getLogger(GitSyncService.class);
    
    @Value("${git.auto-sync:false}")
    private boolean autoSyncEnabled;
    
    @Value("${git.repo-path:.}")
    private String repoPath;
    
    /**
     * Asynchronously commit and push changes to the repository
     * @param message Commit message
     * @return CompletableFuture with result of the operation
     */
    @Async
    public CompletableFuture<Boolean> syncChanges(String message) {
        if (!autoSyncEnabled) {
            logger.info("Git auto-sync is disabled");
            return CompletableFuture.completedFuture(false);
        }
        
        try {
            // Stage all changes
            executeCommand("git -C " + repoPath + " add --all");
            
            // Commit changes
            executeCommand("git -C " + repoPath + " commit -m \"" + message + "\"");
            
            // Push changes
            executeCommand("git -C " + repoPath + " push");
            
            logger.info("Successfully committed and pushed changes: " + message);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            logger.error("Failed to sync changes: " + e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    private String executeCommand(String command) throws Exception {
        Process process = Runtime.getRuntime().exec(command);
        
        // Read output
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        // Read errors
        StringBuilder error = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                error.append(line).append("\n");
            }
        }
        
        // Wait for the process to complete
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            logger.error("Command execution failed: " + command);
            logger.error("Error output: " + error.toString());
            throw new Exception("Command execution failed with exit code " + exitCode + ": " + error.toString());
        }
        
        return output.toString();
    }
}