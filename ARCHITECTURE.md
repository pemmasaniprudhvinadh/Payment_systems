# Payment System Architecture - Event-Driven Design

## High-Level Architecture Flow

```
                        API Gateway (8080)
                              ↓
                    Payment Service (8082)
                              ↓
            Kafka Topic: "payment-events"
                    (Central Event Hub)
         ↙           ↓              ↓              ↘
   Fraud Service   Audit Service   Notification   Settlement Service
      (8083)         (8087)         Service         (8085)
                                    (8086)
```

## Detailed Event Flow

### 1. **Payment Initiation**

```
Client Request
    ↓
API Gateway (/payments) routes to Payment Service (8082)
    ↓
Payment Service validates request
    - Check idempotency key (avoid duplicates)
    - Validate user, card token, amount
    - Generate payment ID
    ↓
Payment Service publishes "PaymentRequested" event to "payment-events" Kafka topic
    ↓
Listeners receive the event in parallel:
├── Fraud Service consumes PaymentRequested
├── Audit Service consumes PaymentRequested (logs it)
└── Notification Service consumes PaymentRequested (sends email)
```

### 2. **Fraud Detection**

```
Fraud Service receives PaymentRequested event
    ↓
Performs fraud analysis:
  - Amount check
  - Token validation
  - Risk scoring (0.0-1.0)
  ↓
Publishes "FraudCheckCompleted" event to "payment-events" topic
    ↓
Listeners receive the event:
├── Payment Service updates payment status (FRAUD_CHECK_PENDING → AUTHORIZED or FRAUD_DETECTED)
├── Audit Service logs fraud analysis result
└── Notification Service sends fraud alert (if flagged)
```

### 3. **Payment Authorization**

```
Auth Service receives FraudCheckCompleted event
    ↓
Checks if payment was flagged as fraud:
  - If fraud detected: Declines authorization
  - If passed: Authorizes payment (90% success rate)
  ↓
Publishes "PaymentAuthorized" event to "payment-events" topic
    ↓
Listeners receive the event:
├── Payment Service updates payment status (FRAUD_CHECK_PENDING → SETTLEMENT_PENDING or AUTHORIZATION_FAILED)
├── Settlement Service prepares for settlement
├── Audit Service logs authorization result
└── Notification Service sends authorization confirmation
```

### 4. **Payment Settlement**

```
Settlement Service receives PaymentAuthorized event
    ↓
Checks authorization status:
  - If not authorized: Skips settlement
  - If authorized: Initiates settlement processing
  ↓
Simulates settlement (90% success rate):
  - Generates transaction ID
  - Records settlement time
  ↓
Publishes "PaymentSettled" event to "payment-events" topic
    ↓
Listeners receive the event:
├── Payment Service updates payment status (SETTLEMENT_PENDING → SETTLED or SETTLEMENT_FAILED)
├── Audit Service logs settlement result
└── Notification Service sends completion confirmation
```

## Service Responsibilities

### **Payment Service (8082)**

- **Role**: Orchestrator and API Gateway
- **Responsibilities**:
  - Receives payment requests from clients
  - Validates and creates payment records
  - Publishes PaymentRequested event
  - Listens for FraudCheckCompleted, PaymentAuthorized, PaymentSettled events
  - Updates payment status based on events
  - Returns payment status to clients
- **Database**: PostgreSQL (payment_db)
- **Ports**: 8082
- **Kafka Topics**:
  - Publishes to: payment-events (PaymentRequested)
  - Consumes from: payment-events (FraudCheckCompleted, PaymentAuthorized, PaymentSettled)

### **Fraud Service (8083)**

- **Role**: Fraud Detection & Risk Assessment
- **Responsibilities**:
  - Consumes PaymentRequested events
  - Analyzes payment for fraud risk
  - Calculates risk score (0.0-1.0)
  - Publishes FraudCheckCompleted event
  - Implements fail-open fallback (non-fraudulent)
- **Database**: None (stateless)
- **Ports**: 8083
- **Kafka Topics**:
  - Consumes from: payment-events (PaymentRequested)
  - Publishes to: payment-events (FraudCheckCompleted)

### **Auth Service (8081)**

- **Role**: Payment Authorization
- **Responsibilities**:
  - Consumes FraudCheckCompleted events
  - Authorizes payments (90% success rate)
  - Publishes PaymentAuthorized event
  - Implements fail-open fallback (authorized)
- **Database**: None (stateless)
- **Ports**: 8081
- **Kafka Topics**:
  - Consumes from: payment-events (FraudCheckCompleted)
  - Publishes to: payment-events (PaymentAuthorized)

### **Settlement Service (8085)**

- **Role**: Payment Settlement & Transaction Finalization
- **Responsibilities**:
  - Consumes PaymentAuthorized events
  - Processes settlement (90% success rate)
  - Generates transaction IDs
  - Publishes PaymentSettled event
  - Persists settlement records
- **Database**: PostgreSQL (payment_db)
- **Ports**: 8085
- **Kafka Topics**:
  - Consumes from: payment-events (PaymentAuthorized)
  - Publishes to: payment-events (PaymentSettled)

### **Notification Service (8086)** ✨ NEW

- **Role**: Customer Communication
- **Responsibilities**:
  - Consumes all payment events from payment-events topic
  - Sends email notifications for each event:
    - Payment initiated
    - Fraud alert (if flagged)
    - Payment authorized
    - Payment completed
  - Implements circuit breaker for email service unavailability
- **Database**: None (stateless, events are ephemeral)
- **Ports**: 8086
- **Kafka Topics**:
  - Consumes from: payment-events (all event types)

### **Audit Service (8087)** ✨ NEW

- **Role**: Compliance & Audit Trail
- **Responsibilities**:
  - Consumes all payment events from payment-events topic
  - Logs all events to audit_logs table
  - Maintains immutable audit trail
  - Provides compliance reporting capability
  - Tracks event type, timestamp, user, amount, status
- **Database**: PostgreSQL (payment_db) - Table: audit_logs
- **Ports**: 8087
- **Kafka Topics**:
  - Consumes from: payment-events (all event types)

### **Card Service (8084)**

- **Role**: Payment Card Tokenization
- **Responsibilities**:
  - Tokenizes card data securely
  - Never stores raw card numbers
  - Provides tokens for payment requests
- **Database**: None (tokenization only)
- **Ports**: 8084
- **API Endpoint**: POST /cards/tokenize

## Kafka Topics Structure

### **payment-events Topic**

Central hub for all payment-related events. Contains 4 event types:

1. **PaymentRequested**
   - Publisher: Payment Service
   - Consumers: Fraud Service, Audit Service, Notification Service
   - Data: paymentId, userId, amount, currency, cardToken, etc.

2. **FraudCheckCompleted**
   - Publisher: Fraud Service
   - Consumers: Payment Service, Audit Service, Notification Service, Auth Service
   - Data: paymentId, userId, isFraudulent, riskScore, reason

3. **PaymentAuthorized**
   - Publisher: Auth Service
   - Consumers: Payment Service, Settlement Service, Audit Service, Notification Service
   - Data: paymentId, userId, authorized, authCode, reason

4. **PaymentSettled**
   - Publisher: Settlement Service
   - Consumers: Payment Service, Audit Service, Notification Service
   - Data: paymentId, userId, transactionId, status

## Circuit Breaker Configuration

All services implement Resilience4j circuit breakers:

| Service              | Circuit Breaker                    | Threshold   | Wait Time |
| -------------------- | ---------------------------------- | ----------- | --------- |
| Payment Service      | fraudCheckCircuitBreaker           | 50% failure | 10s       |
| Payment Service      | authorizationCircuitBreaker        | 50% failure | 10s       |
| Payment Service      | settlementCircuitBreaker           | 50% failure | 10s       |
| Fraud Service        | fraudAnalysisCircuitBreaker        | 50% failure | 5s        |
| Settlement Service   | settlementProcessingCircuitBreaker | 50% failure | 10s       |
| Auth Service         | authorizationCircuitBreaker        | 50% failure | 10s       |
| Notification Service | notificationCircuitBreaker         | 50% failure | 5s        |
| Audit Service        | auditCircuitBreaker                | 50% failure | 5s        |

## Resilience & Fault Tolerance

### Fail-Open Strategies

- **Fraud Service**: Publishes "not fraudulent" when unavailable → Payment proceeds
- **Auth Service**: Publishes "authorized" when unavailable → Payment proceeds
- **Advantage**: Prevents fraud/auth service outages from blocking legitimate payments

### Fail-Safe Strategies

- **Settlement Service**: Logs failure, waits for recovery → Manual intervention if needed
- **Audit Service**: Logs to DLQ if database unavailable → Audit trail preserved
- **Advantage**: Ensures accurate settlement and compliance records

## Advantages of Event-Driven Architecture

1. **Decoupling**: Services don't need to know about each other
2. **Scalability**: Each service can scale independently
3. **Resilience**: One service's failure doesn't cascade to others
4. **Auditability**: All events logged for compliance
5. **Notifications**: Real-time customer updates
6. **Extensibility**: Easy to add new services consuming from payment-events topic

## Example: Adding a Loyalty Points Service

To add a new "Loyalty Points Service" that credits points for successful payments:

```java
@Component
public class PaymentEventListener {
    @KafkaListener(topics = "payment-events", groupId = "loyalty-service")
    public void onPaymentSettled(PaymentEvents.PaymentSettled event) {
        // Credit loyalty points when payment completes
        loyaltyService.creditPoints(event.userId, event.transactionId);
    }
}
```

No changes needed to other services!

## Deployment Ports

| Service              | Port | Purpose                    |
| -------------------- | ---- | -------------------------- |
| API Gateway          | 8080 | Client entry point         |
| Auth Service         | 8081 | Payment authorization      |
| Payment Service      | 8082 | Payment orchestration      |
| Fraud Service        | 8083 | Fraud detection            |
| Card Service         | 8084 | Card tokenization          |
| Settlement Service   | 8085 | Payment settlement         |
| Notification Service | 8086 | Customer notifications     |
| Audit Service        | 8087 | Audit trail logging        |
| Discovery Server     | 8761 | Service discovery (Eureka) |
| Config Server        | 8888 | Configuration management   |

## Running the System

```bash
# Start all services using Docker Compose
docker-compose up

# Scale a specific service
docker-compose up --scale fraud-service=3

# Check service health
curl http://localhost:8761/eureka/apps

# View payment status
curl http://localhost:8082/payments/{paymentId}

# Initiate a payment
curl -X POST http://localhost:8080/payment-service/payments \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "tokenizedCardId": "token_xyz",
    "amount": 99.99,
    "currency": "USD",
    "merchantId": "merchant_123",
    "idempotencyKey": "request_unique_key"
  }'
```

---

**Last Updated**: May 24, 2026
