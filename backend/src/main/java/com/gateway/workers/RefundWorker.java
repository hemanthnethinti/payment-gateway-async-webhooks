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
import com.gateway.jobs.ProcessRefundJob;
import com.gateway.jobs.DeliverWebhookJob;
import com.gateway.models.Payment;
import com.gateway.models.Refund;
import com.gateway.services.PaymentService;
import com.gateway.services.RefundService;
import com.gateway.services.WebhookService;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(prefix = "app.worker", name = "enabled", havingValue = "true", matchIfMissing = false)
public class RefundWorker {
    private static final Logger logger = LoggerFactory.getLogger(RefundWorker.class);
    private static final long REFUND_DELAY_MIN_MS = 3000;
    private static final long REFUND_DELAY_MAX_MS = 5000;

    @Autowired
    private JedisPool jedisPool;

    @Autowired
    private RefundService refundService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private WebhookService webhookService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.jobs.refund-queue:refund_queue}")
    private String refundQueueKey;

    @Value("${app.jobs.webhook-queue:webhook_queue}")
    private String webhookQueueKey;

    /**
     * Consume and process refund jobs from Redis queue.
     * Runs periodically to pull jobs and execute them.
     */
    @Scheduled(fixedDelay = 500, initialDelay = 1000)
    public void processRefundJobs() {
        try (Jedis jedis = jedisPool.getResource()) {
            String jobJson = jedis.lpop(refundQueueKey);
            if (jobJson == null) {
                return;
            }

            ProcessRefundJob job = objectMapper.readValue(jobJson, ProcessRefundJob.class);
            processRefundJob(job, jedis);
        } catch (Exception e) {
            logger.error("Error processing refund jobs", e);
        }
    }

    /**
     * Process a single refund job.
     *
     * @param job   The refund job to process
     * @param jedis Redis connection
     */
    private void processRefundJob(ProcessRefundJob job, Jedis jedis) {
        try {
            logger.info("Processing refund job: {}", job.getRefundId());

            // Fetch refund from database
            Refund refund = refundService.getRefundById(job.getRefundId());
            if (refund == null) {
                logger.error("Refund not found: {}", job.getRefundId());
                return;
            }

            // Fetch payment
            Payment payment = paymentService.getPaymentById(refund.getPaymentId());
            if (payment == null) {
                logger.error("Payment not found: {}", refund.getPaymentId());
                return;
            }

            // Verify payment is refundable
            if (!"success".equalsIgnoreCase(payment.getStatus())) {
                logger.error("Payment not in refundable state: {} - status: {}", 
                    refund.getPaymentId(), payment.getStatus());
                return;
            }

            // Verify total refunded amount doesn't exceed payment amount
            long totalRefunded = refundService.getTotalRefundedAmount(payment.getId());
            if (totalRefunded + refund.getAmount() > payment.getAmount()) {
                logger.error("Refund amount exceeds available amount for payment: {}", payment.getId());
                return;
            }

            // Simulate refund processing delay (3-5 seconds)
            long delayMs = REFUND_DELAY_MIN_MS + 
                (long) (Math.random() * (REFUND_DELAY_MAX_MS - REFUND_DELAY_MIN_MS));
            logger.info("Simulating refund processing delay: {} ms", delayMs);
            Thread.sleep(delayMs);

            // Update refund status to processed
            refund.setStatus("processed");
            refund.setProcessedAt(Instant.now());
            refundService.updateRefund(refund);

            logger.info("Refund processed successfully: {}", job.getRefundId());

            // Enqueue webhook event for refund.processed
            enqueueWebhookEvent(refund, payment, jedis);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Refund processing interrupted", e);
        } catch (Exception e) {
            logger.error("Error processing refund job: {}", job.getRefundId(), e);
        }
    }

    /**
     * Enqueue a webhook event for refund processed.
     *
     * @param refund  The refund
     * @param payment The associated payment
     * @param jedis   Redis connection
     */
    private void enqueueWebhookEvent(Refund refund, Payment payment, Jedis jedis) {
        try {
            // Create webhook payload
            Map<String, Object> refundData = new HashMap<>();
            refundData.put("id", refund.getId());
            refundData.put("payment_id", refund.getPaymentId());
            refundData.put("amount", refund.getAmount());
            refundData.put("reason", refund.getReason());
            refundData.put("status", refund.getStatus());
            refundData.put("created_at", refund.getCreatedAt().toString());
            refundData.put("processed_at", refund.getProcessedAt().toString());

            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("event", "refund.processed");
            payloadMap.put("timestamp", System.currentTimeMillis() / 1000);
            
            Map<String, Object> data = new HashMap<>();
            data.put("refund", refundData);
            data.put("payment", createPaymentData(payment));
            payloadMap.put("data", data);

            var log = new com.gateway.models.WebhookLog(refund.getMerchantId(), "refund.processed", payloadMap);
            webhookService.createWebhookLog(log);

            DeliverWebhookJob webhookJob = new DeliverWebhookJob();
            webhookJob.setWebhookLogId(log.getId());
            webhookJob.setMerchantId(refund.getMerchantId());
            webhookJob.setEventType("refund.processed");
            webhookJob.setPayload(payloadMap);
            webhookJob.setRetryCount(log.getAttempts());

            String jobJson = objectMapper.writeValueAsString(webhookJob);
            jedis.rpush(webhookQueueKey, jobJson);

            logger.info("Webhook event enqueued: refund.processed for refund {}", refund.getId());
        } catch (Exception e) {
            logger.error("Failed to enqueue webhook event", e);
        }
    }

    /**
     * Create payment data for webhook payload.
     *
     * @param payment The payment
     * @return Map containing payment data
     */
    private Map<String, Object> createPaymentData(Payment payment) {
        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("id", payment.getId());
        paymentData.put("order_id", payment.getOrderId());
        paymentData.put("amount", payment.getAmount());
        paymentData.put("currency", payment.getCurrency());
        paymentData.put("method", payment.getMethod());
        if (payment.getVpa() != null) {
            paymentData.put("vpa", payment.getVpa());
        }
        paymentData.put("status", payment.getStatus());
        paymentData.put("created_at", payment.getCreatedAt().toString());
        return paymentData;
    }
}
