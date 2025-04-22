package com.fileshare.oneshot.service;

import com.fileshare.oneshot.util.ConnectionCodeGenerator;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class ConnectionCodeService {
    
    private final ConnectionCodeGenerator codeGenerator = new ConnectionCodeGenerator();
    
    // Keep track of used connection codes
    private final Set<String> activeConnectionCodes = new HashSet<>();

    public String generateConnectionCode(long activeFileCount) {
        // Determine the minimum length of connection code needed
        int minLength = determineMinimumCodeLength(activeFileCount);
        
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
    
    private int determineMinimumCodeLength(long activeFileCount) {
        // Add 1 for the new file being uploaded
        activeFileCount += 1;
        
        // Calculate the minimum code length needed
        int length = 1;
        // Sử dụng phương thức từ ConnectionCodeGenerator để tính toán giới hạn 1%
        long maxAllowedCount = ConnectionCodeGenerator.calculateOnePercentLimit(length);
        
        while (activeFileCount > maxAllowedCount) {
            length++;
            maxAllowedCount = ConnectionCodeGenerator.calculateOnePercentLimit(length);
        }
        
        return length;
    }
    
    public int getActiveConnectionCodesCount() {
        return activeConnectionCodes.size();
    }
}
