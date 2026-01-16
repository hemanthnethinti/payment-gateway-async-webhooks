package com.gateway.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for calculating retry schedules with exponential backoff.
 */
public class RetryScheduleUtil {
    private static final Logger logger = LoggerFactory.getLogger(RetryScheduleUtil.class);

    // Production webhook retry intervals (in seconds)
    private static final long[] PRODUCTION_INTERVALS = {0, 60, 300, 1800, 7200}; // 0s, 1m, 5m, 30m, 2h

    // Test webhook retry intervals (in seconds)
    private static final long[] TEST_INTERVALS = {0, 5, 10, 15, 20};

    private static final int MAX_RETRIES = 5;

    /**
     * Get the retry interval for a given attempt number.
     *
     * @param attemptNumber The attempt number (1-indexed)
     * @param testMode      Whether to use test intervals (shorter delays)
     * @return Delay in seconds before next retry
     */
    public static long getRetryDelay(int attemptNumber, boolean testMode) {
        long[] intervals = testMode ? TEST_INTERVALS : PRODUCTION_INTERVALS;
        
        if (attemptNumber < 1 || attemptNumber > MAX_RETRIES) {
            logger.warn("Invalid attempt number: {}", attemptNumber);
            return 0;
        }
        
        return intervals[attemptNumber - 1];
    }

    /**
     * Calculate exponential backoff delay.
     * Formula: min(base_delay * 2^attempt, max_delay)
     *
     * @param attempt   The attempt number (0-indexed)
     * @param baseDelay Base delay in seconds
     * @param maxDelay  Maximum delay in seconds
     * @return Delay in seconds
     */
    public static long getExponentialBackoffDelay(int attempt, long baseDelay, long maxDelay) {
        if (attempt < 0) {
            return 0;
        }
        
        long delay = (long) (baseDelay * Math.pow(2, attempt));
        return Math.min(delay, maxDelay);
    }

    /**
     * Get all retry intervals for reference.
     *
     * @param testMode Whether to get test or production intervals
     * @return Array of retry intervals in seconds
     */
    public static long[] getRetryIntervals(boolean testMode) {
        return testMode ? TEST_INTERVALS : PRODUCTION_INTERVALS;
    }

    /**
     * Check if a webhook has exceeded max retries.
     *
     * @param attemptCount Current attempt count
     * @return true if max retries exceeded, false otherwise
     */
    public static boolean hasExceededMaxRetries(int attemptCount) {
        return attemptCount >= MAX_RETRIES;
    }

    /**
     * Get maximum number of retries allowed.
     *
     * @return Maximum retry count
     */
    public static int getMaxRetries() {
        return MAX_RETRIES;
    }
}
