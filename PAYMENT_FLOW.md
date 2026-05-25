# Payment Project - Payment Flow Architecture

## Overview

This is a comprehensive microservices-based payment processing system with event-driven architecture using Apache Kafka.

## Payment Flow

The system follows this complete flow:

```
1. Client initiates a payment request
2. Payment API validates request and generates an idempotency key
3. Sensitive card data is tokenized (no raw data stored)
4. A payment event is published to Kafka
5. Fraud and authorization services consume the event
6. Transaction is approved/declined
7. Settlement service completes the transaction
8. Final status is returned to the client
```

## Architecture

### Microservices

#### 1. **Payment Service** (Port 8082)

- **Role**: Main entry point for payment processing
- **Responsibilities**:
  - Validates payment requests
  - Generates idempotency keys for request deduplication
  - Publishes `PaymentRequested` events to Kafka
  - Tracks payment status through the entire flow
  - Consumes completion events from fraud, auth, and settlement services
  - Provides REST API for payment initiation and status check

**Key Endpoints**:

- `POST /payments` - Initiate a payment
- `GET /payments/{paymentId}` - Get payment status

#### 2. **Card Service** (Port 8081)

- **Role**: Card management and tokenization
- **Responsibilities**:
  - Stores card metadata (masked numbers only)
  - Tokenizes card data securely without storing raw card numbers
  - Publishes `CardAdded` events
  - Provides REST API for card management

**Key Endpoints**:

- `POST /cards/tokenize` - Tokenize a card (no raw data stored)
- `POST /cards` - Add card metadata
- `GET /cards` - List cards
- `DELETE /cards/{id}` - Delete card

#### 3. **Fraud Service** (Port 8083)

- **Role**: Fraud detection and risk assessment
- **Responsibilities**:
  - Listens for `PaymentRequested` events
  - Performs fraud analysis (risk scoring, pattern detection)
  - Publishes `FraudCheckCompleted` events

**Fraud Checks**:

- High transaction amount detection
- Card token validation
- Risk scoring (0.0 - 1.0 scale)

#### 4. **Auth Service** (Port 8081)

- **Role**: Payment authorization
- **Responsibilities**:
  - Listens for `FraudCheckCompleted` events
  - Authorizes payments if not flagged as fraudulent
  - Integrates with payment processors
  - Publishes `PaymentAuthorized` events

#### 5. **Settlement Service** (Port 8085)

- **Role**: Transaction settlement and completion
- **Responsibilities**:
  - Listens for `PaymentAuthorized` events
  - Processes settlement (fund transfer)
  - Handles settlement success/failure
  - Publishes `PaymentSettled` events

#### 6. **API Gateway** (Port 8080)

- Routes requests to appropriate microservices
- Service discovery integration

#### 7. **Config Server** (Port 8888)

- Centralized configuration management

#### 8. **Discovery Server** (Port 8761)

- Service registration and discovery (Eureka)

## Event-Driven Flow

The system uses Apache Kafka topics for asynchronous communication:

### Kafka Topics

1. **payment-requested**
   - Published by: Payment Service
   - Consumed by: Fraud Service
   - Data: User ID, card token, amount, merchant ID, idempotency key

2. **fraud-check-completed**
   - Published by: Fraud Service
   - Consumed by: Auth Service, Payment Service
   - Data: Payment ID, fraud status, risk score, reason

3. **payment-authorized**
   - Published by: Auth Service
   - Consumed by: Settlement Service, Payment Service
   - Data: Payment ID, authorization status, auth code

4. **payment-settled**
   - Published by: Settlement Service
   - Consumed by: Payment Service
   - Data: Payment ID, settlement ID, transaction ID, settlement status

5. **card-added**
   - Published by: Card Service
   - Consumed by: Loan Service
   - Data: Card ID, user ID, masked number

## Payment Status States

```
PENDING
  ↓
FRAUD_CHECK_PENDING
  ├→ FRAUD_DETECTED (if fraudulent)
  │   └→ Payment rejected
  ├→ AUTHORIZED (if not fraudulent)
     └→ SETTLEMENT_PENDING
        ├→ SETTLED (successful)
        └→ SETTLEMENT_FAILED (failed)
```

## Data Security

### Card Data Protection

- **No raw card data stored** in any service
- Card numbers are tokenized via Card Service
- Only masked card numbers (last 4 digits) stored in database
- Tokens are passed through payment flow, never raw card data

### Idempotency

- All payment requests include an idempotency key
- System prevents duplicate processing even on network retries
- Clients can safely retry failed requests

## API Usage Examples

### 1. Tokenize a Card

```bash
POST http://localhost:8081/cards/tokenize
Content-Type: application/json

{
  "userId": "user123",
  "cardNumber": "4532-1234-5678-9010",
  "expiryDate": "12/25",
  "cvv": "123"
}

Response:
{
  "token": "token_xyz_abc123",
  "last4": "9010",
  "message": "Card tokenized successfully"
}
```

### 2. Initiate a Payment

```bash
POST http://localhost:8082/payments
Content-Type: application/json

{
  "userId": "user123",
  "tokenizedCardId": "token_xyz_abc123",
  "amount": 99.99,
  "currency": "USD",
  "merchantId": "merchant456",
  "idempotencyKey": "req-20250524-001"
}

Response:
{
  "paymentId": "pay_123456",
  "status": "FRAUD_CHECK_PENDING",
  "idempotencyKey": "req-20250524-001",
  "message": "Payment initiated. Awaiting fraud check..."
}
```

### 3. Check Payment Status

```bash
GET http://localhost:8082/payments/pay_123456

Response:
{
  "paymentId": "pay_123456",
  "status": "SETTLED",
  "amount": 99.99,
  "currency": "USD",
  "fraudCheckStatus": "PASSED",
  "fraudRiskScore": 0.15,
  "authCode": "AUTH_abc123xy",
  "transactionId": "TXN_9876543",
  "createdAt": "2025-05-24T10:30:45",
  "updatedAt": "2025-05-24T10:31:02"
}
```

## Deployment

### Using Docker Compose

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

### Services and Ports

- API Gateway: http://localhost:8080
- Card Service: http://localhost:8081
- Payment Service: http://localhost:8082
- Fraud Service: http://localhost:8083
- Settlement Service: http://localhost:8085
- Config Server: http://localhost:8888
- Discovery Server: http://localhost:8761
- PostgreSQL: localhost:5432
- Redis: localhost:6379
- Kafka: localhost:9092
- Zookeeper: localhost:2181

## Infrastructure Requirements

- **PostgreSQL 15**: Database for payment and settlement records
- **Redis 7**: Caching and session management
- **Kafka 7.4.0**: Event streaming
- **Zookeeper 7.4.0**: Kafka coordination
- **Java 17**: Application runtime

## Configuration

### Database

- User: `payment_user`
- Password: `payment_pass`
- Database: `payment_db`
- Host: `localhost:5432`

### Kafka Bootstrap Servers

- `localhost:9092`

### Redis

- Host: `localhost:6379`

## Module Structure

```
payment_project/
├── auth-service/          # Authentication & authorization
├── card-service/          # Card tokenization & management
├── common/                # Shared DTOs and event models
├── config-server/         # Configuration management
├── discovery-server/      # Service discovery (Eureka)
├── api-gateway/           # API gateway & routing
├── payment-service/       # Main payment processing (NEW)
├── fraud-service/         # Fraud detection (NEW)
├── settlement-service/    # Payment settlement (NEW)
├── loan-service/          # Loan processing
└── docker-compose.yml     # Container orchestration
```

## Next Steps

1. **Run the services** using Docker Compose
2. **Test the payment flow** using the provided API examples
3. **Monitor Kafka topics** to observe event flow
4. **Integrate with your frontend** for payment processing
5. **Add monitoring** (Prometheus, Grafana) for production

## Testing the Full Flow

```bash
# 1. Start all services
docker-compose up -d

# 2. Tokenize a card
curl -X POST http://localhost:8081/cards/tokenize \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "cardNumber": "4532-1234-5678-9010",
    "expiryDate": "12/25",
    "cvv": "123"
  }'

# 3. Initiate a payment
curl -X POST http://localhost:8082/payments \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "tokenizedCardId": "token_xyz_abc123",
    "amount": 99.99,
    "currency": "USD",
    "merchantId": "merchant456",
    "idempotencyKey": "req-20250524-001"
  }'

# 4. Monitor the payment status
curl http://localhost:8082/payments/pay_123456

# 5. View Kafka topics (optional)
docker exec -it payment_project-kafka-1 kafka-topics --list --bootstrap-server localhost:9092
```

## Production Considerations

1. **Card Tokenization**: Integrate with real payment processors (Stripe, Square, Adyen)
2. **Fraud Detection**: Implement ML-based fraud detection models
3. **Authorization**: Connect to actual payment processor authorization APIs
4. **PCI Compliance**: Ensure all card data handling is PCI DSS compliant
5. **Monitoring**: Add Prometheus metrics and Grafana dashboards
6. **Logging**: Centralized logging with ELK stack
7. **Security**: Add JWT authentication, SSL/TLS encryption
8. **Database**: Set up replication and backup strategies
9. **Kafka**: Configure cluster with multiple brokers
10. **API Rate Limiting**: Implement rate limiting on payment endpoints

---

**Last Updated**: May 24, 2025
