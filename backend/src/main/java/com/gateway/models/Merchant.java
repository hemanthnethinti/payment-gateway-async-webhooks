package com.gateway.models;

import java.util.Objects;
import java.util.UUID;

/**
 * Merchant entity representing a merchant account.
 */
public class Merchant {
    private String id;
    private String email;
    private String apiKey;
    private String apiSecret;
    private String webhookUrl;
    private String webhookSecret;
    private String name;

    // Constructors
    public Merchant() {
        this.id = UUID.randomUUID().toString();
    }

    public Merchant(String email, String apiKey, String apiSecret) {
        this();
        this.email = email;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Merchant merchant = (Merchant) o;
        return Objects.equals(id, merchant.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Merchant{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
