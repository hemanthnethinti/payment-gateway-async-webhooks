package com.gateway.services.impl;

import com.gateway.models.Merchant;
import com.gateway.services.MerchantService;
import com.gateway.util.IdGenerator;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryMerchantService implements MerchantService {
    private final Map<String, Merchant> merchants = new ConcurrentHashMap<>();
    private final Map<String, String> apiKeyToId = new ConcurrentHashMap<>();
    private final Map<String, String> emailToId = new ConcurrentHashMap<>();

    public InMemoryMerchantService() {
        // Seed a default merchant for testing
        Merchant m = new Merchant();
        m.setId("m_test");
        m.setEmail("test@example.com");
        m.setName("Test Merchant");
        m.setApiKey("key_test_abc123");
        m.setApiSecret("secret_test_xyz789");
        m.setWebhookSecret(IdGenerator.generateWebhookSecret());
        String defaultWebhook = System.getenv().getOrDefault("DEFAULT_WEBHOOK_URL", "");
        if (!defaultWebhook.isBlank()) {
            m.setWebhookUrl(defaultWebhook);
        }
        createMerchant(m);
    }

    @Override
    public Merchant getMerchantById(String merchantId) {
        return merchants.get(merchantId);
    }

    @Override
    public Merchant getMerchantByApiKey(String apiKey) {
        String id = apiKeyToId.get(apiKey);
        return id == null ? null : merchants.get(id);
    }

    @Override
    public Merchant getMerchantByEmail(String email) {
        String id = emailToId.get(email);
        return id == null ? null : merchants.get(id);
    }

    @Override
    public Merchant createMerchant(Merchant merchant) {
        if (merchant.getId() == null || merchant.getId().isEmpty()) {
            merchant.setId(java.util.UUID.randomUUID().toString());
        }
        merchants.put(merchant.getId(), merchant);
        if (merchant.getApiKey() != null) apiKeyToId.put(merchant.getApiKey(), merchant.getId());
        if (merchant.getEmail() != null) emailToId.put(merchant.getEmail(), merchant.getId());
        if (merchant.getWebhookSecret() == null || merchant.getWebhookSecret().isEmpty()) {
            merchant.setWebhookSecret(IdGenerator.generateWebhookSecret());
        }
        return merchant;
    }

    @Override
    public Merchant updateMerchant(Merchant merchant) {
        merchants.put(merchant.getId(), merchant);
        return merchant;
    }

    @Override
    public Merchant verifyCredentials(String apiKey, String apiSecret) {
        Merchant m = getMerchantByApiKey(apiKey);
        if (m != null && apiSecret != null && apiSecret.equals(m.getApiSecret())) {
            return m;
        }
        return null;
    }
}
