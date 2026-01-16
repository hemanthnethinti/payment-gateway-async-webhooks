package com.gateway.services;

import com.gateway.models.Merchant;

/**
 * Service interface for merchant operations.
 */
public interface MerchantService {
    
    /**
     * Get merchant by ID.
     *
     * @param merchantId The merchant ID
     * @return Merchant object or null if not found
     */
    Merchant getMerchantById(String merchantId);
    
    /**
     * Get merchant by API key.
     *
     * @param apiKey The API key
     * @return Merchant object or null if not found
     */
    Merchant getMerchantByApiKey(String apiKey);
    
    /**
     * Get merchant by email.
     *
     * @param email The merchant email
     * @return Merchant object or null if not found
     */
    Merchant getMerchantByEmail(String email);
    
    /**
     * Create a new merchant.
     *
     * @param merchant The merchant to create
     * @return Created merchant
     */
    Merchant createMerchant(Merchant merchant);
    
    /**
     * Update merchant details.
     *
     * @param merchant The merchant to update
     * @return Updated merchant
     */
    Merchant updateMerchant(Merchant merchant);
    
    /**
     * Verify merchant API credentials.
     *
     * @param apiKey    The API key
     * @param apiSecret The API secret
     * @return Merchant if credentials are valid, null otherwise
     */
    Merchant verifyCredentials(String apiKey, String apiSecret);
}
