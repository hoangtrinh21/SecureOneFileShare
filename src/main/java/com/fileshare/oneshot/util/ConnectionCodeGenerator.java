package com.fileshare.oneshot.util;

import java.security.SecureRandom;

public class ConnectionCodeGenerator {
    
    private static final String VALID_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final SecureRandom random = new SecureRandom();
    
    /**
     * Generates a random connection code of the specified length
     * 
     * @param length The length of the connection code to generate
     * @return The generated connection code
     */
    public String generateCode(int length) {
        if (length < 1) {
            throw new IllegalArgumentException("Code length must be at least 1");
        }
        
        StringBuilder codeBuilder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(VALID_CHARS.length());
            codeBuilder.append(VALID_CHARS.charAt(randomIndex));
        }
        
        return codeBuilder.toString();
    }
    
    /**
     * Calculates the maximum number of possible codes for a given length
     * 
     * @param length The length of the code
     * @return The maximum number of possible codes
     */
    public static long calculateMaxCodes(int length) {
        return (long) Math.pow(VALID_CHARS.length(), length);
    }
    
    /**
     * Calculates the 1% limit of possible codes for a given length
     * Theo yêu cầu, chỉ sử dụng 1% số lượng mã tối đa của độ dài tương ứng
     * 
     * @param length The length of the code
     * @return The 1% limit of possible codes
     */
    public static long calculateOnePercentLimit(int length) {
        return Math.max(1, (long) (calculateMaxCodes(length) * 0.01));
    }
}
