package com.gateway.models;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * WebhookLog entity representing a webhook delivery attempt.
 */
public class WebhookLog {
    private String id;
    private String merchantId;
    private String event;
    private Map<String, Object> payload;
    private String status;
    private int attempts;
    private Instant lastAttemptAt;
    private Instant nextRetryAt;
    private Integer responseCode;
    private String responseBody;
    private Instant createdAt;

    // Constructors
    public WebhookLog() {
        this.id = UUID.randomUUID().toString();
        this.status = "pending";
        this.attempts = 0;
        this.createdAt = Instant.now();
    }

    public WebhookLog(String merchantId, String event, Map<String, Object> payload) {
        this();
        this.merchantId = merchantId;
        this.event = event;
        this.payload = payload;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public Instant getLastAttemptAt() {
        return lastAttemptAt;
    }

    public void setLastAttemptAt(Instant lastAttemptAt) {
        this.lastAttemptAt = lastAttemptAt;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(Instant nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public Integer getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(Integer responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WebhookLog that = (WebhookLog) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "WebhookLog{" +
                "id='" + id + '\'' +
                ", merchantId='" + merchantId + '\'' +
                ", event='" + event + '\'' +
                ", status='" + status + '\'' +
                ", attempts=" + attempts +
                ", createdAt=" + createdAt +
                '}';
    }
}
