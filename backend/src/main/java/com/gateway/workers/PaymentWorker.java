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
import com.gateway.jobs.ProcessPaymentJob;
import com.gateway.jobs.DeliverWebhookJob;
import com.gateway.models.Payment;
import com.gateway.services.PaymentService;
import com.gateway.services.WebhookService;
import com.gateway.util.RetryScheduleUtil;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(prefix = "app.worker", name = "enabled", havingValue = "true", matchIfMissing = false)
public class PaymentWorker {
    private static final Logger logger = LoggerFactory.getLogger(PaymentWorker.class);
    private static final int DEFAULT_DELAY_MS = 2000;

    @Autowired
    private JedisPool jedisPool;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private WebhookService webhookService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.payment.test-mode:false}")
    private boolean testMode;

    @Value("${app.payment.max-retries:3}")
    private int maxRetries;

    @Value("${app.jobs.payment-queue:payment_queue}")
    private String paymentQueueKey;

    @Value("${app.jobs.webhook-queue:webhook_queue}")
    private String webhookQueueKey;

    /**
     * Consume and process payment jobs from Redis queue.
     * Runs periodically to pull jobs and execute them.
     */
    @Scheduled(fixedDelay = 500, initialDelay = 1000)
    public void processPaymentJobs() {
        try (Jedis jedis = jedisPool.getResource()) {
            String jobJson = jedis.lpop(paymentQueueKey);
            if (jobJson == null) {
                return;
            }

            ProcessPaymentJob job = objectMapper.readValue(jobJson, ProcessPaymentJob.class);
            processPaymentJob(job, jedis);
        } catch (Exception e) {
            logger.error("Error processing payment jobs", e);
        }
    }

    /**
     * Process a single payment job.
     *
     * @param job  The payment job to process
     * @param jedis Redis connection
     */
    private void processPaymentJob(ProcessPaymentJob job, Jedis jedis) {
        try {
            logger.info("Processing payment job: {}", job.getPaymentId());

            // Fetch payment from database
            Payment payment = paymentService.getPaymentById(job.getPaymentId());
            if (payment == null) {
                logger.error("Payment not found: {}", job.getPaymentId());
                return;
            }

            // Update status to PROCESSING
            payment.setStatus("processing");
            payment.setUpdatedAt(Instant.now());
            paymentService.updatePayment(payment);

            // Handle TEST_MODE with configurable delay
            if (testMode) {
                long delayMs = job.getTestDelayMs() > 0 ? job.getTestDelayMs() : DEFAULT_DELAY_MS;
                logger.info("TEST_MODE: Delaying payment processing by {} ms", delayMs);
                Thread.sleep(delayMs);
            }

            // Process payment (simulate external payment gateway call or logic)
            boolean success = processPaymentWithRetry(payment, job);

            if (success) {
                // Update status to COMPLETED
                payment.setStatus("success");
                payment.setCompletedAt(Instant.now());
                payment.setUpdatedAt(Instant.now());
                paymentService.updatePayment(payment);

                logger.info("Payment processed successfully: {}", job.getPaymentId());

                // Enqueue webhook event for successful payment
                enqueueWebhookEvent(payment, "payment.success", jedis);
                // Metrics
                try { jedis.incr("metrics:payments_completed"); } catch (Exception ignore) {}
            } else {
                // Update status to FAILED
                payment.setStatus("failed");
                payment.setFailedAt(Instant.now());
                payment.setUpdatedAt(Instant.now());
                paymentService.updatePayment(payment);

                logger.warn("Payment processing failed: {}", job.getPaymentId());

                // Enqueue webhook event for failed payment
                enqueueWebhookEvent(payment, "payment.failed", jedis);
                // Metrics
                try { jedis.incr("metrics:payments_failed"); } catch (Exception ignore) {}

                // Schedule retry if max retries not exceeded
                if (job.getRetryCount() < maxRetries) {
                    scheduleRetry(job, jedis);
                }
            }

        } catch (Exception e) {
            logger.error("Error processing payment job: {}", job.getPaymentId(), e);
            
            // Attempt to enqueue error webhook
            try {
                Payment payment = paymentService.getPaymentById(job.getPaymentId());
                if (payment != null) {
                    payment.setStatus("error");
                    payment.setFailedAt(Instant.now());
                    payment.setUpdatedAt(Instant.now());
                    paymentService.updatePayment(payment);
                    enqueueWebhookEvent(payment, "payment.error", jedis);
                }
            } catch (Exception innerE) {
                logger.error("Failed to handle payment error webhook", innerE);
            }
        }
    }

    /**
     * Process payment with retry logic.
     * Simulates different success rates based on payment method.
     *
     * @param payment The payment to process
     * @param job     The original job
     * @return true if successful, false otherwise
     */
    private boolean processPaymentWithRetry(Payment payment, ProcessPaymentJob job) {
        // In TEST_MODE, honor test success flag
        if (testMode && !job.isTestSuccess()) {
            logger.info("TEST_MODE: Payment simulation failed for {}", job.getPaymentId());
            return false;
        }

        if (testMode && job.isTestSuccess()) {
            logger.info("TEST_MODE: Payment simulation succeeded for {}", job.getPaymentId());
            return true;
        }

        // Simulate payment processing logic with method-based success rates
        try {
            // Simulate processing time (5-10 seconds)
            long delayMs = 5000 + (long) (Math.random() * 5000);
            Thread.sleep(delayMs);
            
            // Determine success rate based on payment method
            double successRate = "upi".equalsIgnoreCase(payment.getMethod()) ? 0.9 : 0.95;
            boolean isSuccess = Math.random() < successRate;
            
            if (!isSuccess) {
                logger.warn("Payment processing failed, will retry: {}", job.getPaymentId());
                // Set error details
                payment.setErrorCode("PAYMENT_FAILED");
                payment.setErrorDescription("Payment processing failed");
            }
            
            return isSuccess;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Payment processing interrupted", e);
            payment.setErrorCode("PROCESSING_INTERRUPTED");
            payment.setErrorDescription("Payment processing was interrupted");
            return false;
        }
    }

    /**
     * Enqueue a webhook event for delivery.
     *
     * @param payment  The payment associated with the webhook
     * @param eventType The type of webhook event
     * @param jedis    Redis connection
     */
    private void enqueueWebhookEvent(Payment payment, String eventType, Jedis jedis) {
        try {
            // Build payload
            Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("event", eventType);
            payload.put("paymentId", payment.getId());
            payload.put("orderId", payment.getOrderId());
            payload.put("status", payment.getStatus());
            payload.put("amount", payment.getAmount());
            payload.put("currency", payment.getCurrency());
            payload.put("method", payment.getMethod());
            payload.put("merchantId", payment.getMerchantId());
            payload.put("timestamp", Instant.now().toString());

            // Create webhook log entry
            var log = new com.gateway.models.WebhookLog(payment.getMerchantId(), eventType, payload);
            log = webhookService.createWebhookLog(log);

            // Create job for delivery
            DeliverWebhookJob job = new DeliverWebhookJob();
            job.setWebhookLogId(log.getId());
            job.setMerchantId(payment.getMerchantId());
            job.setEventType(eventType);
            job.setPayload(payload);
            job.setTimestamp(Instant.now());
            job.setRetryCount(0);

            String jobJson = objectMapper.writeValueAsString(job);
            jedis.rpush(webhookQueueKey, jobJson);

            logger.info("Webhook event enqueued: {} for payment {}", eventType, payment.getId());
        } catch (Exception e) {
            logger.error("Failed to enqueue webhook event", e);
        }
    }

    /**
     * Schedule a retry for a failed payment job.
     *
     * @param job   The job to retry
     * @param jedis Redis connection
     */
    private void scheduleRetry(ProcessPaymentJob job, Jedis jedis) {
        try {
            job.setRetryCount(job.getRetryCount() + 1);
            
            // Calculate delay using exponential backoff
            long delaySeconds = RetryScheduleUtil.getExponentialBackoffDelay(
                job.getRetryCount(),
                5, // base delay in seconds
                300 // max delay in seconds
            );

            String jobJson = objectMapper.writeValueAsString(job);
            
            // Add to sorted set with score = current time + delay
            long targetTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(delaySeconds);
            jedis.zadd(paymentQueueKey + ":retry", targetTime, jobJson);

            logger.info("Retry scheduled for payment {} in {} seconds (attempt {})",
                job.getPaymentId(), delaySeconds, job.getRetryCount());
        } catch (Exception e) {
            logger.error("Failed to schedule retry", e);
        }
    }

    /**
     * Process delayed retry jobs from the sorted set.
     * Runs periodically to check and re-queue ready jobs.
     */
    @Scheduled(fixedDelay = 1000, initialDelay = 2000)
    public void processDelayedRetries() {
        try (Jedis jedis = jedisPool.getResource()) {
            long now = System.currentTimeMillis();
            
            // Get all jobs that should be retried now
            var retryJobs = jedis.zrangeByScore(
                paymentQueueKey + ":retry",
                0,
                now
            );

            for (String jobJson : retryJobs) {
                jedis.rpush(paymentQueueKey, jobJson);
                jedis.zrem(paymentQueueKey + ":retry", jobJson);
                logger.info("Retry job moved back to main queue");
            }
        } catch (Exception e) {
            logger.error("Error processing delayed retries", e);
        }
    }
}
