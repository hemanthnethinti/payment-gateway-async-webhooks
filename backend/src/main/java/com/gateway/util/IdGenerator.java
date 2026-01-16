package com.gateway.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.Random;

/**
 * Utility class for generating unique IDs in the payment gateway system.
 */
public class IdGenerator {
    private static final Logger logger = LoggerFactory.getLogger(IdGenerator.class);
    private static final Random RANDOM = new SecureRandom();
    private static final String ALPHANUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    /**
     * Generate a payment ID.
     * Format: "pay_" + 16 alphanumeric characters
     *
     * @return Unique payment ID
     */
    public static String generatePaymentId() {
        return "pay_" + generateRandomString(16);
    }

    /**
     * Generate a refund ID.
     * Format: "rfnd_" + 16 alphanumeric characters
     *
     * @return Unique refund ID
     */
    public static String generateRefundId() {
        return "rfnd_" + generateRandomString(16);
    }

    /**
     * Generate an order ID.
     * Format: "order_" + 16 alphanumeric characters
     *
     * @return Unique order ID
     */
    public static String generateOrderId() {
        return "order_" + generateRandomString(16);
    }

    /**
     * Generate a webhook secret.
     * Format: "whsec_" + 32 alphanumeric characters
     *
     * @return Unique webhook secret
     */
    public static String generateWebhookSecret() {
        return "whsec_" + generateRandomString(32);
    }

    /**
     * Generate a random alphanumeric string of specified length.
     *
     * @param length Length of the random string to generate
     * @return Random alphanumeric string
     */
    public static String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }
}
