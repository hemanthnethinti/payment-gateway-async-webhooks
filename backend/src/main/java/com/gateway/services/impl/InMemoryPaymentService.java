package com.gateway.services.impl;

import com.gateway.models.Payment;
import com.gateway.services.PaymentService;
import com.gateway.util.IdGenerator;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryPaymentService implements PaymentService {
    private final Map<String, Payment> payments = new ConcurrentHashMap<>();

    @Override
    public Payment getPaymentById(String paymentId) {
        return payments.get(paymentId);
    }

    @Override
    public Payment createPayment(Payment payment) {
        if (payment.getId() == null || payment.getId().isEmpty()) {
            payment.setId(IdGenerator.generatePaymentId());
        }
        if (payment.getCreatedAt() == null) payment.setCreatedAt(Instant.now());
        payment.setUpdatedAt(Instant.now());
        payments.put(payment.getId(), payment);
        return payment;
    }

    @Override
    public Payment updatePayment(Payment payment) {
        payment.setUpdatedAt(Instant.now());
        payments.put(payment.getId(), payment);
        return payment;
    }

    @Override
    public List<Payment> getPaymentsByMerchant(String merchantId, int limit, int offset) {
        List<Payment> all = new ArrayList<>();
        for (Payment p : payments.values()) {
            if (merchantId == null || merchantId.equals(p.getMerchantId())) {
                all.add(p);
            }
        }
        int from = Math.min(offset, all.size());
        int to = Math.min(from + limit, all.size());
        return all.subList(from, to);
    }

    @Override
    public long getTotalRefundedAmount(String paymentId) {
        // In-memory stub: not tracking refunds here
        return 0;
    }
}
