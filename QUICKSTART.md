# Quick Start Guide - Payment Flow System

## Prerequisites

- Docker & Docker Compose
- Java 17
- Gradle

## Quick Start (5 minutes)

### 1. Start All Services

```bash
cd /Users/prudhvipemmasani/Documents/payment_project
docker-compose up -d
```

### 2. Build All Modules

```bash
./gradlew clean build -x test
```

### 3. Test the Payment Flow

**Step 1: Tokenize a Card**

```bash
curl -X POST http://localhost:8081/cards/tokenize \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test_user_001",
    "cardNumber": "4532-1234-5678-9010",
    "expiryDate": "12/25",
    "cvv": "123"
  }'
```

**Response**: Get the `token` (e.g., `token_abc123def456`)

**Step 2: Initiate Payment**

```bash
curl -X POST http://localhost:8082/payments \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test_user_001",
    "tokenizedCardId": "token_abc123def456",
    "amount": 150.50,
    "currency": "USD",
    "merchantId": "merchant_789",
    "idempotencyKey": "unique-req-001"
  }'
```

**Response**: Get the `paymentId` (e.g., `pay_xyz789`)

**Step 3: Check Payment Status**

```bash
curl http://localhost:8082/payments/pay_xyz789
```

Monitor the status as it progresses through: `FRAUD_CHECK_PENDING` → `AUTHORIZED` → `SETTLEMENT_PENDING` → `SETTLED`

## Service Ports

| Service                   | Port | URL                   |
| ------------------------- | ---- | --------------------- |
| API Gateway               | 8080 | http://localhost:8080 |
| Card Service              | 8081 | http://localhost:8081 |
| Payment Service           | 8082 | http://localhost:8082 |
| Fraud Service             | 8083 | http://localhost:8083 |
| Settlement Service        | 8085 | http://localhost:8085 |
| Config Server             | 8888 | http://localhost:8888 |
| Discovery Server (Eureka) | 8761 | http://localhost:8761 |
| PostgreSQL                | 5432 | localhost:5432        |
| Kafka                     | 9092 | localhost:9092        |

## Monitoring

### View Service Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f payment-service
docker-compose logs -f fraud-service
docker-compose logs -f settlement-service
```

### Check Kafka Topics

```bash
# List all topics
docker exec -it payment_project-kafka-1 \
  kafka-topics --list --bootstrap-server localhost:9092

# Monitor payment events
docker exec -it payment_project-kafka-1 \
  kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic payment-requested --from-beginning
```

### Check Database

```bash
# Connect to PostgreSQL
psql -h localhost -U payment_user -d payment_db -W
# Password: payment_pass

# Check payments table
SELECT * FROM payments;

# Check settlements table
SELECT * FROM settlements;
```

## Troubleshooting

### Issue: Services not connecting

**Solution**: Ensure all containers are running:

```bash
docker-compose ps
```

### Issue: Database connection errors

**Solution**: Wait for PostgreSQL to be ready (takes ~10 seconds):

```bash
docker-compose up -d
sleep 15  # Wait for database to initialize
./gradlew clean build -x test
```

### Issue: Port already in use

**Solution**: Stop conflicting services:

```bash
docker-compose down
lsof -i :8082  # Check what's using the port
kill -9 <PID>  # Kill the process
```

## Key Files

- `PAYMENT_FLOW.md` - Detailed architecture documentation
- `settings.gradle` - Gradle multi-module configuration
- `docker-compose.yml` - Infrastructure setup
- `payment-service/` - Main payment processing logic
- `fraud-service/` - Fraud detection logic
- `settlement-service/` - Settlement processing logic

## Common Commands

```bash
# View all services
docker-compose ps

# Stop all services
docker-compose down

# View specific service logs
docker-compose logs -f payment-service

# Remove all data (careful!)
docker-compose down -v

# Rebuild specific service
./gradlew payment-service:build

# Run tests
./gradlew test

# Clean build
./gradlew clean build -x test
```

## Payment Status Flow Diagram

```
Client Request
    ↓
Payment Service (8082)
    ↓ [Publish: payment-requested]
Kafka
    ↓
Fraud Service (8083)
    ↓ [Publish: fraud-check-completed]
Kafka
    ↓
Auth Service (8081)
    ↓ [Publish: payment-authorized]
Kafka
    ↓
Settlement Service (8085)
    ↓ [Publish: payment-settled]
Kafka
    ↓
Payment Service (8082)
    ↓
Client Response (Status: SETTLED)
```

---

For more details, see `PAYMENT_FLOW.md`
