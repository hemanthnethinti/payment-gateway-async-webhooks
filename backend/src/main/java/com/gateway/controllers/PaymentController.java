package com.gateway.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.jobs.ProcessPaymentJob;
import com.gateway.models.IdempotencyKey;
import com.gateway.models.Merchant;
import com.gateway.models.Payment;
import com.gateway.services.MerchantService;
import com.gateway.services.PaymentService;
import com.gateway.util.IdGenerator;
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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/payments")
@CrossOrigin(origins = "*")
public class PaymentController {
	private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

	private final PaymentService paymentService;
	private final MerchantService merchantService;
	private final JedisPool jedisPool;
	private final ObjectMapper objectMapper;
	private final Map<String, IdempotencyKey> idempotencyStore = new ConcurrentHashMap<>();

	@Value("${app.jobs.payment-queue:payment_queue}")
	private String paymentQueueKey;

	@Value("${app.payment.test-mode:false}")
	private boolean testMode;

	@Value("${app.payment.test-processing-delay:0}")
	private long testProcessingDelay;

	@Value("${app.payment.test-payment-success:true}")
	private boolean testPaymentSuccess;

	public PaymentController(PaymentService paymentService,
							 MerchantService merchantService,
							 JedisPool jedisPool,
							 ObjectMapper objectMapper) {
		this.paymentService = paymentService;
		this.merchantService = merchantService;
		this.jedisPool = jedisPool;
		this.objectMapper = objectMapper;
	}

	@PostMapping
	public ResponseEntity<?> createPayment(@RequestHeader(value = "X-Api-Key", required = false) String apiKey,
										   @RequestHeader(value = "X-Api-Secret", required = false) String apiSecret,
										   @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
										   @RequestBody PaymentRequest request) {
		Merchant merchant = authenticate(apiKey, apiSecret);
		if (merchant == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("invalid_api_credentials", "Invalid API key or secret"));
		}

		if (!StringUtils.hasText(request.getOrderId())) {
			return ResponseEntity.badRequest().body(error("missing_order_id", "order_id is required"));
		}
		if (!StringUtils.hasText(request.getMethod())) {
			return ResponseEntity.badRequest().body(error("missing_method", "method is required"));
		}

		// Idempotency check: merchant + key
		if (StringUtils.hasText(idempotencyKey)) {
			String compositeKey = merchant.getId() + ":" + idempotencyKey;
			IdempotencyKey cached = idempotencyStore.get(compositeKey);
			if (cached != null && cached.isValid()) {
				logger.info("Returning cached idempotent response for key {}", compositeKey);
				return ResponseEntity.status(HttpStatus.OK).body(cached.getResponse());
			}
		}

		Payment payment = new Payment();
		payment.setId(IdGenerator.generatePaymentId());
		payment.setOrderId(request.getOrderId());
		payment.setMerchantId(merchant.getId());
		payment.setAmount(Optional.ofNullable(request.getAmount()).orElse(50000L));
		payment.setCurrency(Optional.ofNullable(request.getCurrency()).orElse("INR"));
		payment.setMethod(request.getMethod());
		payment.setVpa(request.getVpa());
		payment.setCardNumber(maskCard(request.getCardNumber()));
		payment.setCaptured(false);
		payment.setStatus("pending");
		payment.setIdempotencyKey(idempotencyKey);
		payment.setCreatedAt(Instant.now());
		payment.setUpdatedAt(Instant.now());

		paymentService.createPayment(payment);

		enqueuePaymentJob(payment);

		Map<String, Object> response = paymentResponse(payment);

		// Persist idempotent response
		if (StringUtils.hasText(idempotencyKey)) {
			String compositeKey = merchant.getId() + ":" + idempotencyKey;
			IdempotencyKey cacheEntry = new IdempotencyKey(idempotencyKey, merchant.getId(), response);
			idempotencyStore.put(compositeKey, cacheEntry);
		}

		return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
	}

	@GetMapping("/{paymentId}")
	public ResponseEntity<?> getPayment(@RequestHeader(value = "X-Api-Key", required = false) String apiKey,
										@RequestHeader(value = "X-Api-Secret", required = false) String apiSecret,
										@PathVariable String paymentId) {
		Merchant merchant = authenticate(apiKey, apiSecret);
		if (merchant == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("invalid_api_credentials", "Invalid API key or secret"));
		}

		Payment payment = paymentService.getPaymentById(paymentId);
		if (payment == null || !merchant.getId().equals(payment.getMerchantId())) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("payment_not_found", "Payment not found"));
		}

		return ResponseEntity.ok(paymentResponse(payment));
	}

	@GetMapping("/test/{paymentId}")
	public ResponseEntity<?> getPaymentTest(@RequestHeader(value = "X-Api-Key", required = false) String apiKey,
											@RequestHeader(value = "X-Api-Secret", required = false) String apiSecret,
											@PathVariable String paymentId) {
		return getPayment(apiKey, apiSecret, paymentId);
	}

	private Merchant authenticate(String apiKey, String apiSecret) {
		if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(apiSecret)) {
			return null;
		}
		return merchantService.verifyCredentials(apiKey, apiSecret);
	}

	private void enqueuePaymentJob(Payment payment) {
		ProcessPaymentJob job = new ProcessPaymentJob(payment.getId(), payment.getMerchantId());
		if (testMode) {
			job.setTestDelayMs(testProcessingDelay);
			job.setTestSuccess(testPaymentSuccess);
		}

		try (Jedis jedis = jedisPool.getResource()) {
			String jobJson = objectMapper.writeValueAsString(job);
			jedis.rpush(paymentQueueKey, jobJson);
			logger.info("Payment job enqueued for payment {}", payment.getId());
		} catch (Exception e) {
			logger.error("Failed to enqueue payment job", e);
		}
	}

	private Map<String, Object> paymentResponse(Payment payment) {
		Map<String, Object> data = new HashMap<>();
		data.put("id", payment.getId());
		data.put("order_id", payment.getOrderId());
		data.put("merchant_id", payment.getMerchantId());
		data.put("amount", payment.getAmount());
		data.put("currency", payment.getCurrency());
		data.put("method", payment.getMethod());
		if (payment.getVpa() != null) data.put("vpa", payment.getVpa());
		if (payment.getCardNumber() != null) data.put("card_last4", last4(payment.getCardNumber()));
		data.put("status", payment.getStatus());
		data.put("created_at", payment.getCreatedAt());
		data.put("updated_at", payment.getUpdatedAt());
		if (payment.getCompletedAt() != null) data.put("completed_at", payment.getCompletedAt());
		if (payment.getFailedAt() != null) data.put("failed_at", payment.getFailedAt());
		return data;
	}

	private Map<String, Object> error(String code, String message) {
		Map<String, Object> err = new HashMap<>();
		err.put("error", code);
		err.put("message", message);
		return err;
	}

	private String maskCard(String cardNumber) {
		if (!StringUtils.hasText(cardNumber)) {
			return null;
		}
		String trimmed = cardNumber.replaceAll("\\s+", "");
		if (trimmed.length() <= 4) {
			return trimmed;
		}
		String last4 = trimmed.substring(trimmed.length() - 4);
		return "**** **** **** " + last4;
	}

	private String last4(String masked) {
		if (!StringUtils.hasText(masked)) return null;
		if (masked.length() >= 4) {
			return masked.substring(masked.length() - 4);
		}
		return masked;
	}

	/**
	 * Simple DTO to avoid leaking internal model.
	 */
	public static class PaymentRequest {
        @com.fasterxml.jackson.annotation.JsonProperty("order_id")
        private String orderId;
        private Long amount;
        private String currency;
        private String method;
        private String vpa;
        @com.fasterxml.jackson.annotation.JsonProperty("card_number")
        private String cardNumber;

		public String getOrderId() {
			return orderId;
		}

		public void setOrderId(String orderId) {
			this.orderId = orderId;
		}

		public Long getAmount() {
			return amount;
		}

		public void setAmount(Long amount) {
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
	}
}
