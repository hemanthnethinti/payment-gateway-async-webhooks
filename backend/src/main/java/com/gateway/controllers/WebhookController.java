package com.gateway.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.jobs.DeliverWebhookJob;
import com.gateway.models.Merchant;
import com.gateway.models.WebhookLog;
import com.gateway.services.MerchantService;
import com.gateway.services.WebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks")
@CrossOrigin(origins = "*")
public class WebhookController {
	private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

	private final MerchantService merchantService;
	private final WebhookService webhookService;
	private final JedisPool jedisPool;
	private final ObjectMapper objectMapper;

	@Value("${app.jobs.webhook-queue:webhook_queue}")
	private String webhookQueueKey;

	public WebhookController(MerchantService merchantService,
							 WebhookService webhookService,
							 JedisPool jedisPool,
							 ObjectMapper objectMapper) {
		this.merchantService = merchantService;
		this.webhookService = webhookService;
		this.jedisPool = jedisPool;
		this.objectMapper = objectMapper;
	}

	@GetMapping
	public ResponseEntity<?> listWebhooks(@RequestHeader(value = "X-Api-Key", required = false) String apiKey,
										  @RequestHeader(value = "X-Api-Secret", required = false) String apiSecret,
										  @RequestParam(value = "limit", defaultValue = "10") int limit,
										  @RequestParam(value = "offset", defaultValue = "0") int offset) {
		Merchant merchant = authenticate(apiKey, apiSecret);
		if (merchant == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("invalid_api_credentials", "Invalid API key or secret"));
		}

		List<WebhookLog> logs = webhookService.getWebhookLogs(merchant.getId(), limit, offset);
		Map<String, Object> response = new HashMap<>();
		response.put("items", logs);
		response.put("count", logs.size());
		response.put("limit", limit);
		response.put("offset", offset);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/{webhookId}")
	public ResponseEntity<?> getWebhook(@RequestHeader(value = "X-Api-Key", required = false) String apiKey,
										@RequestHeader(value = "X-Api-Secret", required = false) String apiSecret,
										@PathVariable String webhookId) {
		Merchant merchant = authenticate(apiKey, apiSecret);
		if (merchant == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("invalid_api_credentials", "Invalid API key or secret"));
		}

		WebhookLog log = webhookService.getWebhookLogById(webhookId);
		if (log == null || !merchant.getId().equals(log.getMerchantId())) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("webhook_not_found", "Webhook log not found"));
		}
		return ResponseEntity.ok(log);
	}

	@PostMapping("/{webhookId}/retry")
	public ResponseEntity<?> retryWebhook(@RequestHeader(value = "X-Api-Key", required = false) String apiKey,
										  @RequestHeader(value = "X-Api-Secret", required = false) String apiSecret,
										  @PathVariable String webhookId) {
		Merchant merchant = authenticate(apiKey, apiSecret);
		if (merchant == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("invalid_api_credentials", "Invalid API key or secret"));
		}

		WebhookLog log = webhookService.getWebhookLogById(webhookId);
		if (log == null || !merchant.getId().equals(log.getMerchantId())) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("webhook_not_found", "Webhook log not found"));
		}

		DeliverWebhookJob job = new DeliverWebhookJob();
		job.setWebhookLogId(log.getId());
		job.setMerchantId(log.getMerchantId());
		job.setEventType(log.getEvent());
		job.setPayload(log.getPayload());
		job.setRetryCount(log.getAttempts());
		job.setTimestamp(Instant.now());

		// Update status to pending for retry
		log.setStatus("pending");
		log.setNextRetryAt(null);
		webhookService.updateWebhookLog(log);

		try (Jedis jedis = jedisPool.getResource()) {
			String jobJson = objectMapper.writeValueAsString(job);
			jedis.rpush(webhookQueueKey, jobJson);
			logger.info("Webhook retry enqueued for {}", webhookId);
		} catch (Exception e) {
			logger.error("Failed to enqueue webhook retry", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error("retry_failed", e.getMessage()));
		}

		Map<String, Object> response = new HashMap<>();
		response.put("webhook_id", webhookId);
		response.put("status", "queued");
		return ResponseEntity.ok(response);
	}

	private Merchant authenticate(String apiKey, String apiSecret) {
		if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(apiSecret)) return null;
		return merchantService.verifyCredentials(apiKey, apiSecret);
	}

	private Map<String, Object> error(String code, String message) {
		Map<String, Object> err = new HashMap<>();
		err.put("error", code);
		err.put("message", message);
		return err;
	}
}
