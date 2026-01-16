package com.gateway.models;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * IdempotencyKey entity for caching API responses and preventing duplicate requests.
 */
public class IdempotencyKey {
    private String key;
    private String merchantId;
    private Map<String, Object> response;
    private Instant createdAt;
    private Instant expiresAt;

    // Constructors
    public IdempotencyKey() {}

    public IdempotencyKey(String key, String merchantId, Map<String, Object> response) {
        this.key = key;
        this.merchantId = merchantId;
        this.response = response;
        this.createdAt = Instant.now();
        this.expiresAt = createdAt.plusSeconds(86400); // 24 hours
    }

    // Getters and Setters
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public Map<String, Object> getResponse() {
        return response;
    }

    public void setResponse(Map<String, Object> response) {
        this.response = response;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    /**
     * Check if idempotency key is still valid (not expired).
     *
     * @return true if not expired, false otherwise
     */
    public boolean isValid() {
        return Instant.now().isBefore(expiresAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdempotencyKey that = (IdempotencyKey) o;
        return Objects.equals(key, that.key) &&
               Objects.equals(merchantId, that.merchantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, merchantId);
    }

    @Override
    public String toString() {
        return "IdempotencyKey{" +
                "key='" + key + '\'' +
                ", merchantId='" + merchantId + '\'' +
                ", createdAt=" + createdAt +
                ", expiresAt=" + expiresAt +
                '}';
    }
}
