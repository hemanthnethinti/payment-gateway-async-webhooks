package com.gateway.services.impl;

import com.gateway.models.Refund;
import com.gateway.services.RefundService;
import com.gateway.util.IdGenerator;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryRefundService implements RefundService {
    private final Map<String, Refund> refunds = new ConcurrentHashMap<>();
    private final Map<String, List<Refund>> byPayment = new ConcurrentHashMap<>();

    @Override
    public Refund getRefundById(String refundId) {
        return refunds.get(refundId);
    }

    @Override
    public Refund createRefund(Refund refund) {
        if (refund.getId() == null || refund.getId().isEmpty()) {
            refund.setId(IdGenerator.generateRefundId());
        }
        if (refund.getCreatedAt() == null) refund.setCreatedAt(Instant.now());
        refunds.put(refund.getId(), refund);
        byPayment.computeIfAbsent(refund.getPaymentId(), k -> new ArrayList<>()).add(refund);
        return refund;
    }

    @Override
    public Refund updateRefund(Refund refund) {
        refunds.put(refund.getId(), refund);
        byPayment.computeIfAbsent(refund.getPaymentId(), k -> new ArrayList<>());
        return refund;
    }

    @Override
    public List<Refund> getRefundsByPayment(String paymentId) {
        return byPayment.getOrDefault(paymentId, List.of());
    }

    @Override
    public long getTotalRefundedAmount(String paymentId) {
        return getRefundsByPayment(paymentId).stream().mapToLong(Refund::getAmount).sum();
    }
}
