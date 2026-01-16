package com.gateway.jobs;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/**
 * Background job for delivering webhook events to merchant endpoints.
 * Consumed by WebhookWorker from Redis queue.
 */
public class DeliverWebhookJob implements Serializable {
    private static final long serialVersionUID = 1L;

    private String webhookLogId;
    private String merchantId;
    private String eventType;
    private Map<String, Object> payload;
    private int retryCount;
    private Instant timestamp;
    private Instant createdAt;

    // Constructors
    public DeliverWebhookJob() {
        this.retryCount = 0;
        this.timestamp = Instant.now();
        this.createdAt = Instant.now();
    }

    public DeliverWebhookJob(String merchantId, String eventType, Map<String, Object> payload) {
        this();
        this.merchantId = merchantId;
        this.eventType = eventType;
        this.payload = payload;
    }

    // Getters and Setters
    public String getWebhookLogId() {
        return webhookLogId;
    }

    public void setWebhookLogId(String webhookLogId) {
        this.webhookLogId = webhookLogId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "DeliverWebhookJob{" +
                "webhookLogId='" + webhookLogId + '\'' +
                ", merchantId='" + merchantId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", retryCount=" + retryCount +
                ", timestamp=" + timestamp +
                ", createdAt=" + createdAt +
                '}';
    }
}
