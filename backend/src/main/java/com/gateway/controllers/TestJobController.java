package com.gateway.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/test/jobs")
@CrossOrigin(origins = "*")
public class TestJobController {
	private static final Logger logger = LoggerFactory.getLogger(TestJobController.class);

	private final JedisPool jedisPool;

	@Value("${app.jobs.payment-queue:payment_queue}")
	private String paymentQueueKey;

	@Value("${app.jobs.webhook-queue:webhook_queue}")
	private String webhookQueueKey;

	@Value("${app.jobs.refund-queue:refund_queue}")
	private String refundQueueKey;

	public TestJobController(JedisPool jedisPool) {
		this.jedisPool = jedisPool;
	}

	@GetMapping("/status")
	public ResponseEntity<Map<String, Object>> getJobStatus() {
		Map<String, Object> status = new HashMap<>();
		long nowEpoch = Instant.now().getEpochSecond();

		try (Jedis jedis = jedisPool.getResource()) {
			// Queue lengths
			long paymentPending = jedis.llen(paymentQueueKey);
			long webhookPending = jedis.llen(webhookQueueKey);
			long refundPending = jedis.llen(refundQueueKey);

			// Retry sets
			String paymentRetryKey = paymentQueueKey + ":retry";
			long paymentRetryTotal = jedis.zcard(paymentRetryKey);
			long paymentRetryDue = jedis.zcount(paymentRetryKey, 0, nowEpoch);

			String webhookRetryKey = webhookQueueKey + ":retry"; // if used in future
			long webhookRetryTotal = jedis.exists(webhookRetryKey) ? jedis.zcard(webhookRetryKey) : 0;
			long webhookRetryDue = jedis.exists(webhookRetryKey) ? jedis.zcount(webhookRetryKey, 0, nowEpoch) : 0;

			status.put("payment_queue_pending", paymentPending);
			status.put("refund_queue_pending", refundPending);
			status.put("webhook_queue_pending", webhookPending);
			status.put("payment_retry_total", paymentRetryTotal);
			status.put("payment_retry_due", paymentRetryDue);
			status.put("webhook_retry_total", webhookRetryTotal);
			status.put("webhook_retry_due", webhookRetryDue);

			// Placeholder counters (to be populated by workers in future patch)
			status.put("payments_completed", jedis.exists("metrics:payments_completed") ? Long.parseLong(jedis.get("metrics:payments_completed")) : 0);
			status.put("payments_failed", jedis.exists("metrics:payments_failed") ? Long.parseLong(jedis.get("metrics:payments_failed")) : 0);
			status.put("webhooks_delivered", jedis.exists("metrics:webhooks_delivered") ? Long.parseLong(jedis.get("metrics:webhooks_delivered")) : 0);
			status.put("webhooks_failed", jedis.exists("metrics:webhooks_failed") ? Long.parseLong(jedis.get("metrics:webhooks_failed")) : 0);
		} catch (Exception e) {
			logger.error("Error fetching job status from Redis", e);
			status.put("error", e.getMessage());
		}

		return ResponseEntity.ok(status);
	}
}
