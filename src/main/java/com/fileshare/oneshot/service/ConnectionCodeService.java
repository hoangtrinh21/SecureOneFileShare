package com.fileshare.oneshot.service;

import com.fileshare.oneshot.util.ConnectionCodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class ConnectionCodeService {

    @Autowired
    private FileService fileService;
    
    private final ConnectionCodeGenerator codeGenerator = new ConnectionCodeGenerator();
    
    // Keep track of used connection codes
    private final Set<String> activeConnectionCodes = new HashSet<>();

    public String generateConnectionCode() {
        // Determine the minimum length of connection code needed
        int minLength = determineMinimumCodeLength();
        
        String code;
        do {
            code = codeGenerator.generateCode(minLength);
        } while (activeConnectionCodes.contains(code));
        
        activeConnectionCodes.add(code);
        return code;
    }
    
    public void releaseConnectionCode(String code) {
        activeConnectionCodes.remove(code);
    }
    
    private int determineMinimumCodeLength() {
        // Get count of active files that are waiting for download
        long activeFileCount = fileService.countActiveFiles() + 1; // +1 for the new file being uploaded
        
        // Calculate the minimum code length needed
        int length = 1;
        double maxAllowedCount = Math.pow(62, length) * 0.01; // Only use 1% of available codes
        
        while (activeFileCount > maxAllowedCount) {
            length++;
            maxAllowedCount = Math.pow(62, length) * 0.01;
        }
        
        return length;
    }
    
    public int getActiveConnectionCodesCount() {
        return activeConnectionCodes.size();
    }
}
