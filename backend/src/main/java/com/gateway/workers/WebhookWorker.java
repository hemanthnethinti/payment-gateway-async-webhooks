package com.gateway.workers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.jobs.DeliverWebhookJob;
import com.gateway.models.Merchant;
import com.gateway.models.WebhookLog;
import com.gateway.services.WebhookService;
import com.gateway.services.MerchantService;
import com.gateway.util.HmacUtil;
import com.gateway.util.RetryScheduleUtil;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(prefix = "app.worker", name = "enabled", havingValue = "true", matchIfMissing = false)
public class WebhookWorker {
    private static final Logger logger = LoggerFactory.getLogger(WebhookWorker.class);
    private static final int WEBHOOK_TIMEOUT_MS = 5000;
    private static final int MAX_RETRIES = 5;

    @Autowired
    private JedisPool jedisPool;

    @Autowired
    private WebhookService webhookService;

    @Autowired
    private MerchantService merchantService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.webhook.test-mode-retries:false}")
    private boolean testModeRetries;

    @Value("${app.jobs.webhook-queue:webhook_queue}")
    private String webhookQueueKey;

    /**
     * Consume and deliver webhook events.
     * Runs periodically to pull webhook jobs and execute them.
     */
    @Scheduled(fixedDelay = 500, initialDelay = 1000)
    public void deliverWebhooks() {
        try (Jedis jedis = jedisPool.getResource()) {
            String jobJson = jedis.lpop(webhookQueueKey);
            if (jobJson == null) {
                return;
            }

            DeliverWebhookJob job = objectMapper.readValue(jobJson, DeliverWebhookJob.class);
            deliverWebhook(job);
        } catch (Exception e) {
            logger.error("Error delivering webhooks", e);
        }
    }

    /**
     * Deliver a single webhook event.
     *
     * @param job The webhook delivery job
     */
    private void deliverWebhook(DeliverWebhookJob job) {
        try {
            logger.info("Delivering webhook: {} for merchant {}", job.getEventType(), job.getMerchantId());

            // Fetch merchant details
            Merchant merchant = merchantService.getMerchantById(job.getMerchantId());
            if (merchant == null) {
                logger.error("Merchant not found: {}", job.getMerchantId());
                return;
            }

            // Skip if webhook URL is not configured
            if (merchant.getWebhookUrl() == null || merchant.getWebhookUrl().trim().isEmpty()) {
                logger.info("Webhook URL not configured for merchant: {}", job.getMerchantId());
                return;
            }

            // Convert payload to JSON string
            String payloadJson = objectMapper.writeValueAsString(job.getPayload());

            // Generate HMAC signature
            String signature = HmacUtil.generateSignature(payloadJson, merchant.getWebhookSecret());

            // Send HTTP POST request
            boolean success = sendWebhookRequest(merchant.getWebhookUrl(), payloadJson, signature, job);

            // Log webhook attempt
            int newAttemptCount = job.getRetryCount() + 1;
            
            if (success) {
                // Mark webhook as delivered
                webhookService.logWebhookSuccess(job.getWebhookLogId(), newAttemptCount);
                logger.info("Webhook delivered successfully: {}", job.getEventType());
                try (Jedis jedis = jedisPool.getResource()) { jedis.incr("metrics:webhooks_delivered"); }
            } else {
                // Handle retry or mark as failed
                if (newAttemptCount < MAX_RETRIES) {
                    // Schedule retry
                    long delaySeconds = RetryScheduleUtil.getRetryDelay(newAttemptCount + 1, testModeRetries);
                    webhookService.scheduleRetry(job.getWebhookLogId(), newAttemptCount, delaySeconds);
                    logger.info("Webhook retry scheduled for attempt {} in {} seconds", newAttemptCount + 1, delaySeconds);
                } else {
                    // Mark as permanently failed
                    webhookService.logWebhookFailed(job.getWebhookLogId(), newAttemptCount);
                    logger.warn("Webhook delivery failed permanently after {} attempts", newAttemptCount);
                    try (Jedis jedis = jedisPool.getResource()) { jedis.incr("metrics:webhooks_failed"); }
                }
            }

        } catch (Exception e) {
            logger.error("Error processing webhook job", e);
        }
    }

    /**
     * Send webhook HTTP POST request.
     *
     * @param webhookUrl  The merchant's webhook endpoint URL
     * @param payload     The JSON payload
     * @param signature   The HMAC signature
     * @param job         The webhook job
     * @return true if successful (HTTP 200-299), false otherwise
     */
    private boolean sendWebhookRequest(String webhookUrl, String payload, String signature, DeliverWebhookJob job) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(webhookUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(WEBHOOK_TIMEOUT_MS);
            connection.setReadTimeout(WEBHOOK_TIMEOUT_MS);
            
            // Set headers
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("X-Webhook-Signature", signature);
            
            // Send payload
            connection.setDoOutput(true);
            try (var os = connection.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
            
            // Check response
            int responseCode = connection.getResponseCode();
            String responseBody = readResponse(connection);
            
            // Log response
            webhookService.logWebhookAttempt(job.getWebhookLogId(), job.getRetryCount() + 1, responseCode, responseBody);
            
            logger.info("Webhook response: {} - {}", responseCode, responseBody);
            
            // Return true if successful (200-299)
            return responseCode >= 200 && responseCode < 300;
            
        } catch (Exception e) {
            logger.error("Failed to send webhook request to {}", webhookUrl, e);
            webhookService.logWebhookAttempt(job.getWebhookLogId(), job.getRetryCount() + 1, null, e.getMessage());
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Read response from HTTP connection.
     *
     * @param connection The HTTP connection
     * @return Response body as string (truncated to 1000 chars)
     */
    private String readResponse(HttpURLConnection connection) {
        try {
            int responseCode = connection.getResponseCode();
            var inputStream = responseCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
            
            if (inputStream == null) {
                return "";
            }
            
            byte[] buffer = new byte[1024];
            int bytesRead = inputStream.read(buffer);
            
            if (bytesRead == -1) {
                return "";
            }
            
            String response = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
            return response.length() > 1000 ? response.substring(0, 1000) : response;
            
        } catch (Exception e) {
            logger.warn("Failed to read webhook response", e);
            return "";
        }
    }

    /**
     * Process delayed webhook retries.
     * Runs periodically to check and re-queue ready jobs.
     */
    @Scheduled(fixedDelay = 1000, initialDelay = 2000)
    public void processDelayedRetries() {
        try {
            // Find webhook logs with pending retries
            var pendingRetries = webhookService.getPendingRetries();
            
            for (WebhookLog webhookLog : pendingRetries) {
                // Recreate and enqueue the job
                DeliverWebhookJob job = new DeliverWebhookJob();
                job.setWebhookLogId(webhookLog.getId());
                job.setMerchantId(webhookLog.getMerchantId());
                job.setEventType(webhookLog.getEvent());
                job.setPayload(webhookLog.getPayload());
                job.setRetryCount(webhookLog.getAttempts());
                
                try (Jedis jedis = jedisPool.getResource()) {
                    String jobJson = objectMapper.writeValueAsString(job);
                    jedis.rpush(webhookQueueKey, jobJson);
                    logger.info("Retry webhook moved back to queue: {}", webhookLog.getId());
                }
            }
        } catch (Exception e) {
            logger.error("Error processing delayed webhook retries", e);
        }
    }
}
