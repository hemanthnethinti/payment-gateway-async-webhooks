package com.gateway.services.impl;

import com.gateway.models.WebhookLog;
import com.gateway.services.WebhookService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryWebhookService implements WebhookService {
    private final Map<String, WebhookLog> logs = new ConcurrentHashMap<>();

    @Override
    public WebhookLog createWebhookLog(WebhookLog webhookLog) {
        logs.put(webhookLog.getId(), webhookLog);
        return webhookLog;
    }

    @Override
    public WebhookLog getWebhookLogById(String webhookLogId) {
        return logs.get(webhookLogId);
    }

    @Override
    public WebhookLog updateWebhookLog(WebhookLog webhookLog) {
        logs.put(webhookLog.getId(), webhookLog);
        return webhookLog;
    }

    @Override
    public void logWebhookAttempt(String webhookLogId, int attemptCount, Integer responseCode, String responseBody) {
        WebhookLog log = logs.get(webhookLogId);
        if (log == null) return;
        log.setAttempts(attemptCount);
        log.setLastAttemptAt(Instant.now());
        log.setResponseCode(responseCode);
        log.setResponseBody(responseBody);
        logs.put(webhookLogId, log);
    }

    @Override
    public void logWebhookSuccess(String webhookLogId, int attemptCount) {
        WebhookLog log = logs.get(webhookLogId);
        if (log == null) return;
        log.setAttempts(attemptCount);
        log.setStatus("delivered");
        log.setLastAttemptAt(Instant.now());
        log.setNextRetryAt(null);
        logs.put(webhookLogId, log);
    }

    @Override
    public void logWebhookFailed(String webhookLogId, int attemptCount) {
        WebhookLog log = logs.get(webhookLogId);
        if (log == null) return;
        log.setAttempts(attemptCount);
        log.setStatus("failed");
        log.setLastAttemptAt(Instant.now());
        log.setNextRetryAt(null);
        logs.put(webhookLogId, log);
    }

    @Override
    public void scheduleRetry(String webhookLogId, int attemptCount, long delaySeconds) {
        WebhookLog log = logs.get(webhookLogId);
        if (log == null) return;
        log.setAttempts(attemptCount);
        log.setStatus("pending");
        log.setNextRetryAt(Instant.now().plusSeconds(delaySeconds));
        logs.put(webhookLogId, log);
    }

    @Override
    public List<WebhookLog> getPendingRetries() {
        Instant now = Instant.now();
        List<WebhookLog> result = new ArrayList<>();
        for (WebhookLog l : logs.values()) {
            if (l.getNextRetryAt() != null && !"delivered".equals(l.getStatus()) && !"failed".equals(l.getStatus()) && !l.getNextRetryAt().isAfter(now)) {
                result.add(l);
            }
        }
        return result;
    }

    @Override
    public List<WebhookLog> getWebhookLogs(String merchantId, int limit, int offset) {
        List<WebhookLog> all = new ArrayList<>();
        for (WebhookLog l : logs.values()) {
            if (merchantId == null || merchantId.equals(l.getMerchantId())) {
                all.add(l);
            }
        }
        int from = Math.min(offset, all.size());
        int to = Math.min(from + limit, all.size());
        return all.subList(from, to);
    }
}
