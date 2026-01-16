package com.gateway.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for generating HMAC-SHA256 signatures for webhook authentication.
 */
public class HmacUtil {
    private static final Logger logger = LoggerFactory.getLogger(HmacUtil.class);
    private static final String ALGORITHM = "HmacSHA256";

    /**
     * Generate HMAC-SHA256 signature for webhook payload.
     *
     * @param payload       JSON string representation of the webhook payload (must be exact, no whitespace changes)
     * @param webhookSecret The merchant's webhook secret key
     * @return Hex-encoded signature string (lowercase)
     */
    public static String generateSignature(String payload, String webhookSecret) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(
                webhookSecret.getBytes(StandardCharsets.UTF_8),
                0,
                webhookSecret.getBytes(StandardCharsets.UTF_8).length,
                ALGORITHM
            );
            
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(keySpec);
            
            byte[] messageBytes = payload.getBytes(StandardCharsets.UTF_8);
            byte[] signatureBytes = mac.doFinal(messageBytes);
            
            // Convert to hex string (lowercase)
            return bytesToHex(signatureBytes);
        } catch (Exception e) {
            logger.error("Error generating HMAC signature", e);
            throw new RuntimeException("Failed to generate webhook signature", e);
        }
    }

    /**
     * Convert byte array to hex string.
     *
     * @param bytes Byte array to convert
     * @return Hex string representation (lowercase)
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Verify webhook signature.
     *
     * @param payload       JSON string representation of the webhook payload
     * @param providedSignature The signature provided in X-Webhook-Signature header
     * @param webhookSecret The merchant's webhook secret key
     * @return true if signature is valid, false otherwise
     */
    public static boolean verifySignature(String payload, String providedSignature, String webhookSecret) {
        try {
            String expectedSignature = generateSignature(payload, webhookSecret);
            return constantTimeEquals(expectedSignature, providedSignature);
        } catch (Exception e) {
            logger.error("Error verifying webhook signature", e);
            return false;
        }
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
     *
     * @param a First string
     * @param b Second string
     * @return true if strings are equal, false otherwise
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        
        return constantTimeEquals(aBytes, bBytes);
    }

    /**
     * Constant-time byte array comparison to prevent timing attacks.
     *
     * @param a First byte array
     * @param b Second byte array
     * @return true if arrays are equal, false otherwise
     */
    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        
        return result == 0;
    }
}
