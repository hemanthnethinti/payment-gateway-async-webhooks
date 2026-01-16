package com.gateway.models;

import java.time.Instant;
import java.util.Objects;

/**
 * Refund entity representing a refund transaction.
 */
public class Refund {
    private String id;
    private String paymentId;
    private String merchantId;
    private long amount;
    private String reason;
    private String status;
    private Instant createdAt;
    private Instant processedAt;

    // Constructors
    public Refund() {}

    public Refund(String id, String paymentId, String merchantId, long amount) {
        this.id = id;
        this.paymentId = paymentId;
        this.merchantId = merchantId;
        this.amount = amount;
        this.status = "pending";
        this.createdAt = Instant.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Refund refund = (Refund) o;
        return Objects.equals(id, refund.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Refund{" +
                "id='" + id + '\'' +
                ", paymentId='" + paymentId + '\'' +
                ", amount=" + amount +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
