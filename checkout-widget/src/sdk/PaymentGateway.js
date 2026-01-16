/**
 * Payment Gateway JavaScript SDK
 * Embeddable checkout widget for merchants
 */

class PaymentGateway {
    /**
     * Initialize PaymentGateway instance
     * @param {Object} options Configuration options
     *   - key: Merchant API key (required)
     *   - orderId: Order ID (required)
     *   - amount: Payment amount (optional, from order)
     *   - currency: Currency code (optional, defaults to INR)
     *   - onSuccess: Callback on successful payment (function)
     *   - onFailure: Callback on failed payment (function)
     *   - onClose: Callback when modal is closed (function)
     */
    constructor(options = {}) {
        this.validateOptions(options);
        
        this.key = options.key;
        this.orderId = options.orderId;
        this.amount = options.amount;
        this.currency = options.currency || 'INR';
        
        this.onSuccess = options.onSuccess || (() => {});
        this.onFailure = options.onFailure || (() => {});
        this.onClose = options.onClose || (() => {});
        
        this.modal = null;
        this.iframe = null;
        this.messageHandler = null;
        
        this.gatewayUrl = this.getGatewayUrl();
    }
    
    /**
     * Validate required options
     */
    validateOptions(options) {
        if (!options.key) {
            throw new Error('Payment Gateway: API key is required');
        }
        if (!options.orderId) {
            throw new Error('Payment Gateway: Order ID is required');
        }
    }
    
    /**
     * Get gateway base URL
     */
    getGatewayUrl() {
        // In development, this would be localhost
        // In production, this would be your checkout domain
        if (typeof window !== 'undefined') {
            const protocol = window.location.protocol;
            const hostname = window.location.hostname;
            const port = window.location.port ? `:${window.location.port}` : '';
            return `${protocol}//${hostname}${port}`;
        }
        return 'http://localhost:3001';
    }
    
    /**
     * Open payment modal
     */
    open() {
        // Create modal HTML structure
        this.createModal();
        
        // Add to DOM
        document.body.appendChild(this.modal);
        
        // Set up message listener
        this.setupMessageListener();
        
        // Show modal
        this.modal.style.display = 'flex';
    }
    
    /**
     * Close payment modal
     */
    close() {
        if (this.modal && this.modal.parentNode) {
            this.modal.parentNode.removeChild(this.modal);
        }
        
        if (this.messageHandler) {
            window.removeEventListener('message', this.messageHandler);
        }
        
        this.onClose();
    }
    
    /**
     * Create modal DOM structure
     */
    createModal() {
        // Create modal container
        const modal = document.createElement('div');
        modal.id = 'payment-gateway-modal';
        modal.setAttribute('data-test-id', 'payment-modal');
        modal.style.cssText = `
            display: none;
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background-color: rgba(0, 0, 0, 0.5);
            align-items: center;
            justify-content: center;
            z-index: 10000;
        `;
        
        // Create modal content
        const content = document.createElement('div');
        content.className = 'modal-content';
        content.style.cssText = `
            position: relative;
            width: 90%;
            max-width: 500px;
            height: 600px;
            background-color: white;
            border-radius: 8px;
            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
            overflow: hidden;
        `;
        
        // Create iframe
        const iframe = document.createElement('iframe');
        iframe.setAttribute('data-test-id', 'payment-iframe');
        iframe.src = `${this.gatewayUrl}/checkout?order_id=${encodeURIComponent(this.orderId)}&key=${encodeURIComponent(this.key)}&embedded=true`;
        iframe.style.cssText = `
            width: 100%;
            height: 100%;
            border: none;
        `;
        iframe.allow = 'payment';
        
        // Create close button
        const closeButton = document.createElement('button');
        closeButton.setAttribute('data-test-id', 'close-modal-button');
        closeButton.className = 'close-button';
        closeButton.innerHTML = '×';
        closeButton.style.cssText = `
            position: absolute;
            top: 10px;
            right: 10px;
            width: 30px;
            height: 30px;
            border: none;
            background-color: #f0f0f0;
            border-radius: 50%;
            cursor: pointer;
            font-size: 24px;
            z-index: 10001;
            display: flex;
            align-items: center;
            justify-content: center;
        `;
        closeButton.onclick = () => this.close();
        
        // Assemble modal
        content.appendChild(iframe);
        content.appendChild(closeButton);
        modal.appendChild(content);
        modal.style.display = 'flex';
        
        this.modal = modal;
        this.iframe = iframe;
    }
    
    /**
     * Set up message listener for cross-origin communication
     */
    setupMessageListener() {
        this.messageHandler = (event) => {
            // In production, validate event.origin
            if (!event.data || !event.data.type) {
                return;
            }
            
            switch (event.data.type) {
                case 'payment_success':
                    this.handlePaymentSuccess(event.data.data);
                    break;
                case 'payment_failed':
                    this.handlePaymentFailure(event.data.data);
                    break;
                case 'close_modal':
                    this.close();
                    break;
                default:
                    break;
            }
        };
        
        window.addEventListener('message', this.messageHandler);
    }
    
    /**
     * Handle successful payment
     */
    handlePaymentSuccess(data) {
        console.log('✅ Payment successful:', data);
        this.onSuccess(data);
        this.close();
    }
    
    /**
     * Handle failed payment
     */
    handlePaymentFailure(error) {
        console.error('❌ Payment failed:', error);
        this.onFailure(error);
    }
}

// Expose globally for merchant websites
if (typeof window !== 'undefined') {
    window.PaymentGateway = PaymentGateway;
}

// Export for module bundlers
if (typeof module !== 'undefined' && module.exports) {
    module.exports = PaymentGateway;
}
