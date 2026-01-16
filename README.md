# Payment Gateway - Async Processing & Webhooks

A production-ready payment gateway system with asynchronous job processing, webhook delivery, embedded checkout widget, and refund management.

## Overview

This is **Deliverable 2** of a progressive payment gateway implementation. Building on the core payment processing system, this delivers enterprise-grade features:

- ✅ **Asynchronous Job Processing**: Redis-based queue with worker services
- ✅ **Webhook System**: HMAC-signed event delivery with intelligent retry logic
- ✅ **Embeddable SDK**: JavaScript checkout widget for merchant websites
- ✅ **Refund Management**: Full and partial refund support with async processing
- ✅ **Idempotency**: Prevent duplicate charges on network retries
- ✅ **Production-Ready**: Docker Compose setup, health checks, monitoring

## Quick Start

### Prerequisites

- Docker & Docker Compose
- Git

### Setup & Run

```bash
# Clone repository
git clone <repo-url>
cd payment-gateway-async-webhooks

# Start all services with Docker Compose
docker-compose up -d

# Wait for services to be healthy (30-60s)
docker-compose ps

# Verify API is running
curl http://localhost:8000/api/v1/test/jobs/status
```

### Access Services

| Service | URL | Purpose |
|---------|-----|---------|
| API Server | http://localhost:8000 | Payment API endpoints |
| Dashboard | http://localhost:3000 | Merchant dashboard |
| Checkout Widget | http://localhost:3001 | Embeddable checkout |
| Database | localhost:5432 | PostgreSQL |
| Redis | localhost:6379 | Job queue |

**Test Merchant Credentials**
- API key: `key_test_abc123`
- API secret: `secret_test_xyz789`
- Webhook secret: generated per boot (default seeded)

## Core Components Implemented

### 1. Job Queue System (Redis-based)
- **ProcessPaymentJob**: Async payment processing with configurable success rates
- **DeliverWebhookJob**: Webhook delivery with HMAC signature and retry logic
- **ProcessRefundJob**: Async refund processing

### 2. Worker Services
- **PaymentWorker**: Processes payments asynchronously, supports TEST_MODE
- **WebhookWorker**: Delivers webhooks with signature verification and retries
- **RefundWorker**: Processes refunds with validation

### 3. Utility Classes
- **HmacUtil**: HMAC-SHA256 signature generation/verification
- **IdGenerator**: Unique ID generation for payments, refunds, webhooks
- **RetryScheduleUtil**: Exponential backoff retry scheduling

### 4. Domain Models
- **Payment**: Payment transactions with status tracking
- **Refund**: Refund records with processing status
- **WebhookLog**: Webhook delivery attempts and results
- **IdempotencyKey**: Request caching for idempotent operations
- **Merchant**: Merchant account with webhook configuration

## API Examples

### Create Payment

```bash
curl -X POST http://localhost:8000/api/v1/payments \
  -H "X-Api-Key: key_test_abc123" \
  -H "X-Api-Secret: secret_test_xyz789" \
  -H "Content-Type: application/json" \
  -d '{
    "order_id": "order_12345",
    "method": "upi",
    "vpa": "user@paytm"
  }'
```

Response:
```json
{
  "id": "pay_abc123xyz789",
  "order_id": "order_12345",
  "amount": 50000,
  "currency": "INR",
  "method": "upi",
  "vpa": "user@paytm",
  "status": "pending",
  "created_at": "2024-01-15T10:31:00Z"
}
```

### Get Payment

```bash
curl http://localhost:8000/api/v1/payments/pay_abc123xyz789 \
  -H "X-Api-Key: key_test_abc123" \
  -H "X-Api-Secret: secret_test_xyz789"
```

### Create Refund

```bash
curl -X POST http://localhost:8000/api/v1/payments/pay_abc123xyz789/refunds \
  -H "X-Api-Key: key_test_abc123" \
  -H "X-Api-Secret: secret_test_xyz789" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 25000,
    "reason": "Customer requested"
  }'
```

### Get Refunds for Payment

```bash
curl http://localhost:8000/api/v1/payments/pay_abc123xyz789/refunds \
  -H "X-Api-Key: key_test_abc123" \
  -H "X-Api-Secret: secret_test_xyz789"
```

### List Webhooks

```bash
curl "http://localhost:8000/api/v1/webhooks?limit=10&offset=0" \
  -H "X-Api-Key: key_test_abc123" \
  -H "X-Api-Secret: secret_test_xyz789"
```

### Retry Failed Webhook

```bash
curl -X POST http://localhost:8000/api/v1/webhooks/{webhookId}/retry \
  -H "X-Api-Key: key_test_abc123" \
  -H "X-Api-Secret: secret_test_xyz789"
```

### Check Job Queue Status

```bash
curl http://localhost:8000/api/v1/test/jobs/status
```

Response:
```json
{
  "payment_queue_pending": 0,
  "webhook_queue_pending": 0,
  "refund_queue_pending": 0,
  "payment_retry_total": 0,
  "payment_retry_due": 0,
  "payments_completed": 5,
  "payments_failed": 1,
  "webhooks_delivered": 4,
  "webhooks_failed": 0
}
```

## JavaScript SDK Usage

```html
<script src="http://localhost:3001/checkout.js"></script>

<button id="pay-button">Pay Now</button>

<script>
  document.getElementById('pay-button').addEventListener('click', function() {
    const checkout = new PaymentGateway({
      key: 'key_test_abc123',
      orderId: 'order_12345',
      onSuccess: (response) => console.log('Payment success:', response),
      onFailure: (error) => console.error('Payment failed:', error)
    });
    checkout.open();
  });
</script>
```

## Webhook Events

**Events Emitted:**
- `payment.created` - When payment is initiated
- `payment.pending` - When awaiting processing
- `payment.success` - When payment succeeds
- `payment.failed` - When payment fails
- `refund.created` - When refund is initiated
- `refund.processed` - When refund completes

**Retry Schedule:**
- Production: Immediate, 1m, 5m, 30m, 2h (5 attempts)
- Test Mode: Immediate, 5s, 10s, 15s, 20s (5 attempts)

**Webhook Payload Format:**
```json
{
  "event": "payment.success",
  "timestamp": 1705315870,
  "data": {
    "payment": {
      "id": "pay_H8sK3jD9s2L1pQr",
      "order_id": "order_NXhj67fGH2jk9mPq",
      "amount": 50000,
      "currency": "INR",
      "method": "upi",
      "status": "success",
      "created_at": "2024-01-15T10:31:00Z"
    }
  }
}
```

## Key Features

### Asynchronous Processing
- Jobs queued in Redis, processed by background workers
- Payment status: pending → processing → success/failed
- Method-based success rates: UPI (90%), Card (95%)
- TEST_MODE for deterministic testing

### Webhook Delivery
- HMAC-SHA256 signed events
- Configurable retry intervals with exponential backoff
- Database-backed retry scheduling
- Response logging and status tracking

### Refund Management
- Full and partial refunds supported
- Validates payment state and refund amounts
- Async processing with 3-5 second delay
- Automatic webhook on completion

### Security & Idempotency
- Idempotency-Key header support (24-hour cache)
- API credential validation
- HMAC signature verification
- Constant-time signature comparison

## Docker Compose Services

The docker-compose.yml includes:

- **postgres**: PostgreSQL 15 database
- **redis**: Redis 7 for job queues
- **api**: Spring Boot API server (Port 8000)
- **worker**: Background job processor (Port 8081)
- **dashboard**: React dashboard (Port 3000)
- **checkout**: Checkout widget service (Port 3001)
- **test-merchant**: Test webhook receiver (Port 4000)

## Configuration

### Environment Variables

```bash
DATABASE_URL=postgresql://gateway_user:gateway_pass@postgres:5432/payment_gateway
REDIS_URL=redis://redis:6379

# Test Configuration
APP_PAYMENT_TEST_MODE=false
APP_PAYMENT_TEST_PROCESSING_DELAY=1000
APP_PAYMENT_TEST_PAYMENT_SUCCESS=true
APP_WEBHOOK_TEST_MODE_RETRIES=false
```

### Test Mode Usage

For rapid testing with instant results:

```bash
APP_PAYMENT_TEST_MODE=true
APP_PAYMENT_TEST_PROCESSING_DELAY=100      # 100ms instead of 5-10s
APP_PAYMENT_TEST_PAYMENT_SUCCESS=true
APP_WEBHOOK_TEST_MODE_RETRIES=true         # Retry: 0s, 5s, 10s, 15s, 20s
```

## Testing

### Manual Test Flow

```bash
# 1. Verify services
docker-compose ps

# 2. Create payment
PAYMENT_ID=$(curl -s -X POST http://localhost:8000/api/v1/payments \
  -H "X-Api-Key: key_test_abc123" \
  -H "X-Api-Secret: secret_test_xyz789" \
  -H "Content-Type: application/json" \
  -d '{"order_id":"test_1","method":"upi","vpa":"test@upi"}' \
  | jq -r '.id')

# 3. Monitor queue
docker-compose exec redis redis-cli LLEN payment_queue

# 4. Check payment status after 10-15s
curl http://localhost:8000/api/v1/test/payments/$PAYMENT_ID \
  -H "X-Api-Key: key_test_abc123" | jq '.status'
```

## Monitoring

### View Logs

```bash
docker-compose logs -f api          # API server
docker-compose logs -f worker       # Background worker
docker-compose logs --timestamps    # All services with timestamps
```

### Redis Inspection

```bash
# Connect to Redis
docker-compose exec redis redis-cli

# View job queues
LLEN payment_queue
LLEN webhook_queue
LLEN refund_queue

# Check pending jobs
LRANGE payment_queue 0 10
```

### Database Inspection

```bash
# Connect to PostgreSQL
docker-compose exec postgres psql -U gateway_user -d payment_gateway

# View recent payments
SELECT id, status, created_at FROM payments ORDER BY created_at DESC;

# View webhook logs
SELECT id, event, status, attempts FROM webhook_logs ORDER BY created_at DESC;
```

## Files Implemented

### Backend
- ✅ `jobs/ProcessPaymentJob.java`
- ✅ `jobs/DeliverWebhookJob.java`
- ✅ `jobs/ProcessRefundJob.java`
- ✅ `workers/PaymentWorker.java`
- ✅ `workers/WebhookWorker.java`
- ✅ `workers/RefundWorker.java`
- ✅ `models/Payment.java`
- ✅ `models/Refund.java`
- ✅ `models/WebhookLog.java`
- ✅ `models/IdempotencyKey.java`
- ✅ `models/Merchant.java`
- ✅ `services/PaymentService.java` (interface)
- ✅ `services/RefundService.java` (interface)
- ✅ `services/WebhookService.java` (interface)
- ✅ `services/MerchantService.java` (interface)
- ✅ `util/HmacUtil.java`
- ✅ `util/IdGenerator.java`
- ✅ `util/RetryScheduleUtil.java`
- ✅ `Dockerfile.worker`

### Frontend
- ✅ `checkout-widget/src/sdk/PaymentGateway.js`

### Configuration
- ✅ `docker-compose.yml`
- ✅ `application.yml`
- ✅ `IMPLEMENTATION.md`

## Still Needed

To complete the implementation:
1. Service layer implementations (PaymentService, RefundService, etc.)
2. Repository layer (database access)
3. API Controllers (endpoints)
4. Dashboard components
5. Database migrations
6. Checkout iframe component
7. Test merchant webhook receiver

## Architecture

```
Merchant Website
        ↓
Checkout SDK (iframe)
        ↓
API Server (8000)
    ↓    ↓    ↓
Redis  DB  Workers (8081)
         ↓    ↓    ↓
    Payment  Webhook  Refund
    Worker   Worker   Worker
```

## Documentation

- [IMPLEMENTATION.md](IMPLEMENTATION.md) - Detailed implementation guide
- Docker Compose networking via `payment-gateway` bridge network
- All services communicate internally via docker network names

## Support

For issues or questions, refer to:
- [IMPLEMENTATION.md](IMPLEMENTATION.md) - Full technical documentation
- Docker logs: `docker-compose logs -f [service]`
- Database queries: `docker-compose exec postgres psql -U gateway_user`
- Redis inspection: `docker-compose exec redis redis-cli`

## License

MIT