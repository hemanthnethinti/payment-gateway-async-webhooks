package com.gateway.services;

import com.gateway.models.Refund;
import java.util.List;

/**
 * Service interface for refund operations.
 */
public interface RefundService {
    
    /**
     * Get refund by ID.
     *
     * @param refundId The refund ID
     * @return Refund object or null if not found
     */
    Refund getRefundById(String refundId);
    
    /**
     * Create a new refund.
     *
     * @param refund The refund to create
     * @return Created refund
     */
    Refund createRefund(Refund refund);
    
    /**
     * Update refund status.
     *
     * @param refund The refund to update
     * @return Updated refund
     */
    Refund updateRefund(Refund refund);
    
    /**
     * Get all refunds for a payment.
     *
     * @param paymentId The payment ID
     * @return List of refunds
     */
    List<Refund> getRefundsByPayment(String paymentId);
    
    /**
     * Get total refunded amount for a payment.
     * Sums all processed and pending refunds.
     *
     * @param paymentId The payment ID
     * @return Total refunded amount
     */
    long getTotalRefundedAmount(String paymentId);
}
