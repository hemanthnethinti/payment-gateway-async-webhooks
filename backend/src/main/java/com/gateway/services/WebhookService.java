package com.gateway.services;

import com.gateway.models.WebhookLog;
import java.util.List;

/**
 * Service interface for webhook operations.
 */
public interface WebhookService {
    
    /**
     * Create a webhook log entry.
     *
     * @param webhookLog The webhook log to create
     * @return Created webhook log
     */
    WebhookLog createWebhookLog(WebhookLog webhookLog);
    
    /**
     * Get webhook log by ID.
     *
     * @param webhookLogId The webhook log ID
     * @return WebhookLog object or null if not found
     */
    WebhookLog getWebhookLogById(String webhookLogId);

    /**
     * Update an existing webhook log entry.
     *
     * @param webhookLog The log to update
     * @return Updated webhook log
     */
    WebhookLog updateWebhookLog(WebhookLog webhookLog);
    
    /**
     * Log webhook delivery attempt.
     *
     * @param webhookLogId The webhook log ID
     * @param attemptCount The attempt number
     * @param responseCode HTTP response code
     * @param responseBody Response body
     */
    void logWebhookAttempt(String webhookLogId, int attemptCount, Integer responseCode, String responseBody);
    
    /**
     * Log successful webhook delivery.
     *
     * @param webhookLogId The webhook log ID
     * @param attemptCount The attempt number
     */
    void logWebhookSuccess(String webhookLogId, int attemptCount);
    
    /**
     * Log failed webhook delivery (permanently).
     *
     * @param webhookLogId The webhook log ID
     * @param attemptCount The attempt number
     */
    void logWebhookFailed(String webhookLogId, int attemptCount);
    
    /**
     * Schedule webhook retry.
     *
     * @param webhookLogId The webhook log ID
     * @param attemptCount Current attempt count
     * @param delaySeconds Delay in seconds before retry
     */
    void scheduleRetry(String webhookLogId, int attemptCount, long delaySeconds);
    
    /**
     * Get pending webhook retries.
     * Returns webhook logs that are ready to be retried now.
     *
     * @return List of webhook logs with pending retries
     */
    List<WebhookLog> getPendingRetries();
    
    /**
     * Get webhook logs for a merchant.
     *
     * @param merchantId The merchant ID
     * @param limit      Number of results to return
     * @param offset     Offset for pagination
     * @return List of webhook logs
     */
    List<WebhookLog> getWebhookLogs(String merchantId, int limit, int offset);
}
