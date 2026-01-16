package com.gateway.jobs;

import java.io.Serializable;
import java.time.Instant;

/**
 * Background job for processing refunds asynchronously.
 * Consumed by RefundWorker from Redis queue.
 */
public class ProcessRefundJob implements Serializable {
    private static final long serialVersionUID = 1L;

    private String refundId;
    private String paymentId;
    private String merchantId;
    private int retryCount;
    private Instant createdAt;

    // Constructors
    public ProcessRefundJob() {
        this.retryCount = 0;
        this.createdAt = Instant.now();
    }

    public ProcessRefundJob(String refundId, String paymentId, String merchantId) {
        this();
        this.refundId = refundId;
        this.paymentId = paymentId;
        this.merchantId = merchantId;
    }

    // Getters and Setters
    public String getRefundId() {
        return refundId;
    }

    public void setRefundId(String refundId) {
        this.refundId = refundId;
    }

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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "ProcessRefundJob{" +
                "refundId='" + refundId + '\'' +
                ", paymentId='" + paymentId + '\'' +
                ", merchantId='" + merchantId + '\'' +
                ", retryCount=" + retryCount +
                ", createdAt=" + createdAt +
                '}';
    }
}
