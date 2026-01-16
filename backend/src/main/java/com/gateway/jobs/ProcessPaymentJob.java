package com.gateway.jobs;

import java.io.Serializable;
import java.time.Instant;

/**
 * Background job for processing payments asynchronously.
 * Consumed by PaymentWorker from Redis queue.
 */
public class ProcessPaymentJob implements Serializable {
    private static final long serialVersionUID = 1L;

    private String paymentId;
    private String merchantId;
    private int retryCount;
    private long testDelayMs;
    private boolean testSuccess;
    private Instant createdAt;

    // Constructors
    public ProcessPaymentJob() {
        this.retryCount = 0;
        this.testDelayMs = 0;
        this.testSuccess = true;
        this.createdAt = Instant.now();
    }

    public ProcessPaymentJob(String paymentId, String merchantId) {
        this();
        this.paymentId = paymentId;
        this.merchantId = merchantId;
    }

    // Getters and Setters
    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public long getTestDelayMs() {
        return testDelayMs;
    }

    public void setTestDelayMs(long testDelayMs) {
        this.testDelayMs = testDelayMs;
    }

    public boolean isTestSuccess() {
        return testSuccess;
    }

    public void setTestSuccess(boolean testSuccess) {
        this.testSuccess = testSuccess;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "ProcessPaymentJob{" +
                "paymentId='" + paymentId + '\'' +
                ", merchantId='" + merchantId + '\'' +
                ", retryCount=" + retryCount +
                ", testDelayMs=" + testDelayMs +
                ", testSuccess=" + testSuccess +
                ", createdAt=" + createdAt +
                '}';
    }
}
