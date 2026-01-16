package com.gateway.services;

import com.gateway.models.Payment;
import java.util.List;

/**
 * Service interface for payment operations.
 */
public interface PaymentService {
    
    /**
     * Get payment by ID.
     *
     * @param paymentId The payment ID
     * @return Payment object or null if not found
     */
    Payment getPaymentById(String paymentId);
    
    /**
     * Create a new payment.
     *
     * @param payment The payment to create
     * @return Created payment
     */
    Payment createPayment(Payment payment);
    
    /**
     * Update payment status and details.
     *
     * @param payment The payment to update
     * @return Updated payment
     */
    Payment updatePayment(Payment payment);
    
    /**
     * Get payments for a merchant.
     *
     * @param merchantId The merchant ID
     * @param limit      Number of results to return
     * @param offset     Offset for pagination
     * @return List of payments
     */
    List<Payment> getPaymentsByMerchant(String merchantId, int limit, int offset);
    
    /**
     * Get total refunded amount for a payment.
     *
     * @param paymentId The payment ID
     * @return Total refunded amount
     */
    long getTotalRefundedAmount(String paymentId);
}
