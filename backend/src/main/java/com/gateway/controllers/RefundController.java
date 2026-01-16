package com.gateway.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.jobs.ProcessRefundJob;
import com.gateway.models.Merchant;
import com.gateway.models.Payment;
import com.gateway.models.Refund;
import com.gateway.services.MerchantService;
import com.gateway.services.PaymentService;
import com.gateway.services.RefundService;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class RefundController {
	private static final Logger logger = LoggerFactory.getLogger(RefundController.class);

	private final MerchantService merchantService;
	private final PaymentService paymentService;
	private final RefundService refundService;
	private final JedisPool jedisPool;
	private final ObjectMapper objectMapper;

	@Value("${app.jobs.refund-queue:refund_queue}")
	private String refundQueueKey;

	public RefundController(MerchantService merchantService,
							PaymentService paymentService,
							RefundService refundService,
							JedisPool jedisPool,
							ObjectMapper objectMapper) {
		this.merchantService = merchantService;
		this.paymentService = paymentService;
		this.refundService = refundService;
		this.jedisPool = jedisPool;
		this.objectMapper = objectMapper;
	}

	@PostMapping("/payments/{paymentId}/refunds")
	public ResponseEntity<?> createRefund(@RequestHeader(value = "X-Api-Key", required = false) String apiKey,
										  @RequestHeader(value = "X-Api-Secret", required = false) String apiSecret,
										  @PathVariable String paymentId,
										  @RequestBody RefundRequest request) {
		Merchant merchant = authenticate(apiKey, apiSecret);
		if (merchant == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("invalid_api_credentials", "Invalid API key or secret"));
		}

		Payment payment = paymentService.getPaymentById(paymentId);
		if (payment == null || !merchant.getId().equals(payment.getMerchantId())) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("payment_not_found", "Payment not found"));
		}

		long amount = Optional.ofNullable(request.getAmount()).orElse(0L);
		if (amount <= 0) {
			return ResponseEntity.badRequest().body(error("invalid_amount", "amount must be > 0"));
		}

		long alreadyRefunded = refundService.getTotalRefundedAmount(paymentId);
		if (alreadyRefunded + amount > payment.getAmount()) {
			return ResponseEntity.badRequest().body(error("refund_exceeds_amount", "Refund amount exceeds available balance"));
		}

		if (!"success".equalsIgnoreCase(payment.getStatus())) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(error("payment_not_refundable", "Payment must be in success status"));
		}

		Refund refund = new Refund();
		refund.setId(IdGenerator.generateRefundId());
		refund.setPaymentId(payment.getId());
		refund.setMerchantId(payment.getMerchantId());
		refund.setAmount(amount);
		refund.setReason(request.getReason());
		refund.setStatus("pending");
		refund.setCreatedAt(Instant.now());

		refundService.createRefund(refund);

		enqueueRefundJob(refund);

		return ResponseEntity.status(HttpStatus.ACCEPTED).body(refundResponse(refund, payment));
	}

	@GetMapping("/refunds/{refundId}")
	public ResponseEntity<?> getRefund(@RequestHeader(value = "X-Api-Key", required = false) String apiKey,
									   @RequestHeader(value = "X-Api-Secret", required = false) String apiSecret,
									   @PathVariable String refundId) {
		Merchant merchant = authenticate(apiKey, apiSecret);
		if (merchant == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("invalid_api_credentials", "Invalid API key or secret"));
		}

		Refund refund = refundService.getRefundById(refundId);
		if (refund == null || !merchant.getId().equals(refund.getMerchantId())) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("refund_not_found", "Refund not found"));
		}
		Payment payment = paymentService.getPaymentById(refund.getPaymentId());
		return ResponseEntity.ok(refundResponse(refund, payment));
	}

	@GetMapping("/payments/{paymentId}/refunds")
	public ResponseEntity<?> listRefunds(@RequestHeader(value = "X-Api-Key", required = false) String apiKey,
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

		List<Refund> refunds = refundService.getRefundsByPayment(paymentId);
		Map<String, Object> response = new HashMap<>();
		response.put("payment_id", paymentId);
		response.put("items", refunds.stream().map(r -> refundResponse(r, payment)).toList());
		return ResponseEntity.ok(response);
	}

	private void enqueueRefundJob(Refund refund) {
		ProcessRefundJob job = new ProcessRefundJob(refund.getId(), refund.getPaymentId(), refund.getMerchantId());
		try (Jedis jedis = jedisPool.getResource()) {
			String jobJson = objectMapper.writeValueAsString(job);
			jedis.rpush(refundQueueKey, jobJson);
			logger.info("Refund job enqueued for refund {}", refund.getId());
		} catch (Exception e) {
			logger.error("Failed to enqueue refund job", e);
		}
	}

	private Merchant authenticate(String apiKey, String apiSecret) {
		if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(apiSecret)) return null;
		return merchantService.verifyCredentials(apiKey, apiSecret);
	}

	private Map<String, Object> refundResponse(Refund refund, Payment payment) {
		Map<String, Object> data = new HashMap<>();
		data.put("id", refund.getId());
		data.put("payment_id", refund.getPaymentId());
		data.put("merchant_id", refund.getMerchantId());
		data.put("amount", refund.getAmount());
		data.put("reason", refund.getReason());
		data.put("status", refund.getStatus());
		data.put("created_at", refund.getCreatedAt());
		if (refund.getProcessedAt() != null) data.put("processed_at", refund.getProcessedAt());
		if (payment != null) {
			data.put("payment_status", payment.getStatus());
			data.put("payment_amount", payment.getAmount());
		}
		return data;
	}

	private Map<String, Object> error(String code, String message) {
		Map<String, Object> err = new HashMap<>();
		err.put("error", code);
		err.put("message", message);
		return err;
	}

	public static class RefundRequest {
		private Long amount;
		private String reason;

		public Long getAmount() {
			return amount;
		}

		public void setAmount(Long amount) {
			this.amount = amount;
		}

		public String getReason() {
			return reason;
		}

		public void setReason(String reason) {
			this.reason = reason;
		}
	}
}
