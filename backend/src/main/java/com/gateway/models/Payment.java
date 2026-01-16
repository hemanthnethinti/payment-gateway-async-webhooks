package com.gateway.models;

import java.time.Instant;
import java.util.Objects;

/**
 * Payment entity representing a payment transaction.
 */
public class Payment {
    private String id;
    private String orderId;
    private String merchantId;
    private long amount;
    private String currency;
    private String method;
    private String vpa;
    private String cardNumber;
    private String status;
    private String errorCode;
    private String errorDescription;
    private boolean captured;
    private String idempotencyKey;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;
    private Instant failedAt;

    // Constructors
    public Payment() {}

    public Payment(String id, String orderId, String merchantId, long amount, String currency, String method) {
        this.id = id;
        this.orderId = orderId;
        this.merchantId = merchantId;
        this.amount = amount;
        this.currency = currency;
        this.method = method;
        this.status = "pending";
        this.captured = false;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
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

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getVpa() {
        return vpa;
    }

    public void setVpa(String vpa) {
        this.vpa = vpa;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public boolean isCaptured() {
        return captured;
    }

    public void setCaptured(boolean captured) {
        this.captured = captured;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(Instant failedAt) {
        this.failedAt = failedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Payment payment = (Payment) o;
        return Objects.equals(id, payment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Payment{" +
                "id='" + id + '\'' +
                ", orderId='" + orderId + '\'' +
                ", merchantId='" + merchantId + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", method='" + method + '\'' +
                ", status='" + status + '\'' +
                ", captured=" + captured +
                ", createdAt=" + createdAt +
                '}';
    }
}
