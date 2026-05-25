# Error Handling & Circuit Breaker Implementation Guide

## Overview

The payment system implements comprehensive error handling and circuit breaker patterns using Resilience4j to prevent cascading failures and ensure system stability.

## Architecture Components

### 1. Exception Hierarchy

All exceptions inherit from `PaymentException` for unified handling:

```
PaymentException (base)
├── PaymentValidationException       // 400 Bad Request
├── CardTokenizationException        // 400 Bad Request
├── PaymentNotFoundException         // 404 Not Found
├── FraudCheckServiceUnavailableException    // 503 Service Unavailable
├── AuthorizationServiceUnavailableException // 503 Service Unavailable
├── SettlementServiceUnavailableException    // 503 Service Unavailable
├── ServiceTimeoutException                  // 504 Gateway Timeout
└── ExternalServiceException                 // 502 Bad Gateway
```

### 2. Circuit Breaker Pattern

Circuit breakers prevent cascading failures by stopping requests to failing services:

#### **Circuit States:**

- **CLOSED** (Normal): Requests pass through, failures are tracked
- **OPEN** (Failing): Requests rejected immediately, no calls to service
- **HALF_OPEN** (Testing): Limited requests allowed to test recovery

#### **Configuration (for all services):**

```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        slidingWindowSize: 10 # Track last 10 calls
        minimumNumberOfCalls: 5 # Need 5 calls before measuring
        permittedNumberOfCallsInHalfOpenState: 3 # Allow 3 calls to test
        automaticTransitionFromOpenToHalfOpenEnabled: true
        waitDurationInOpenState: 5s # Try again after 5 seconds
        failureRateThreshold: 50 # Open if >50% fail
        slowCallRateThreshold: 100 # Open if >100% are slow
        slowCallDurationThreshold: 2s # >2s = slow call
```

## Implemented Circuit Breakers

### Payment Service (Port 8082)

**Circuit Breaker 1: fraudCheckCircuitBreaker**

- Protects: Event listener for fraud check completion
- Fallback: Retries with exponential backoff
- Wait time: 10 seconds before retry

**Circuit Breaker 2: authorizationCircuitBreaker**

- Protects: Event listener for authorization results
- Fallback: Retries with exponential backoff
- Wait time: 10 seconds before retry

**Circuit Breaker 3: settlementCircuitBreaker**

- Protects: Event listener for settlement completion
- Fallback: Retries with exponential backoff
- Wait time: 10 seconds before retry

### Fraud Service (Port 8083)

**Circuit Breaker: fraudAnalysisCircuitBreaker**

- Protects: Fraud analysis processing
- Fallback: Publishes default (non-fraudulent) result to prevent payment blocking
- Wait time: 5 seconds before retry

### Settlement Service (Port 8085)

**Circuit Breaker: settlementProcessingCircuitBreaker**

- Protects: Settlement processing operations
- Fallback: Logs failure, waits for recovery
- Wait time: 10 seconds before retry

### Auth Service (Port 8081)

**Circuit Breaker: authorizationCircuitBreaker**

- Protects: Authorization processing
- Fallback: Publishes default (authorized) result to prevent payment blocking (fail open)
- Wait time: 10 seconds before retry

## Retry Logic

All services implement automatic retry with exponential backoff:

```yaml
resilience4j:
  retry:
    configs:
      default:
        maxAttempts: 3 # Try up to 3 times
        waitDuration: 1000 # 1 second between retries
        ignoreExceptions:
          - PaymentValidationException # Don't retry validation errors
```

## Timeout Protection

All async operations have timeout protection:

```yaml
resilience4j:
  timelimiter:
    configs:
      default:
        timeoutDuration: 5s # Max 5 seconds per operation
        cancelRunningFuture: true # Cancel if timeout exceeded
```

## Error Response Format

All error responses follow this consistent format:

```json
{
  "errorCode": "PAYMENT_NOT_FOUND",
  "message": "Payment not found: pay_123",
  "httpStatus": 404,
  "timestamp": "2025-05-24T10:30:45.123456",
  "retryAfter": "Please retry after a few seconds" // For 503 errors
}
```

## Service Failure Scenarios

### Scenario 1: Fraud Service Offline

1. **Payment initiated** → Goes to FRAUD_CHECK_PENDING
2. **Fraud Service Down** → Circuit breaker opens after 5 failed attempts
3. **Fallback Triggered** → Default (non-fraudulent) result published
4. **Payment Proceeds** → Continues to authorization despite fraud check unavailability

**Advantage**: Prevents payment blocking during fraud service outages

### Scenario 2: Authorization Service Offline

1. **Fraud Check Complete** → Auth Service should process
2. **Auth Service Down** → Circuit breaker opens
3. **Fallback Triggered** → Default (authorized) result published
4. **Payment Continues** → Proceeds to settlement

**Advantage**: Fail-open strategy allows payments during auth service outages

### Scenario 3: Settlement Service Offline

1. **Authorization Complete** → Settlement processing begins
2. **Settlement Service Down** → Circuit breaker opens after failures
3. **Fallback Triggered** → Operation logged, waits for recovery
4. **Payment Status** → Shows SETTLEMENT_PENDING

**Advantage**: Allows manual settlement processing during outages

## Monitoring Circuit Breaker Health

### Endpoint: `/actuator/health` (Spring Boot)

Example response:

```json
{
  "status": "UP",
  "components": {
    "circuitBreakerHealthIndicator": {
      "status": "DOWN",
      "details": {
        "fraudCheckCircuitBreaker": {
          "status": "OPEN",
          "failureRateThreshold": 50.0,
          "failureRate": 75.5,
          "slowCallRateThreshold": 100.0,
          "slowCallRate": 0.0,
          "state": "OPEN"
        }
      }
    }
  }
}
```

## Global Exception Handlers

Each service includes `GlobalExceptionHandler` that catches:

1. **PaymentException** → Custom status codes
2. **PaymentValidationException** → 400 Bad Request
3. **ServiceUnavailableException** → 503 with retry hint
4. **ServiceTimeoutException** → 504 Gateway Timeout
5. **PaymentNotFoundException** → 404 Not Found
6. **RuntimeException** → 500 Internal Server Error
7. **Generic Exception** → 500 Internal Server Error with logging

## Best Practices Implemented

### ✅ Idempotency

- Payment requests deduplicated by idempotency key
- Safe to retry failed requests

### ✅ Input Validation

- All inputs validated before processing
- Clear error messages for invalid requests

### ✅ Error Context

- Error codes for easy client handling
- Detailed error messages for debugging
- Timestamps for correlation

### ✅ Graceful Degradation

- Circuit breakers prevent cascading failures
- Fallback strategies allow partial operation
- Services can continue with degraded functionality

### ✅ Logging & Monitoring

- All errors logged with context
- Circuit breaker state changes logged
- Timestamps for debugging

## Testing Circuit Breakers

### Manual Test: Trigger Circuit Breaker

```bash
# 1. Start payment service
docker-compose up -d payment-service

# 2. Stop fraud service
docker-compose stop fraud-service

# 3. Initiate multiple payments (will fail fraud check)
for i in {1..10}; do
  curl -X POST http://localhost:8082/payments \
    -H "Content-Type: application/json" \
    -d '{
      "userId": "user_test",
      "tokenizedCardId": "token_abc123",
      "amount": 100,
      "currency": "USD",
      "merchantId": "merchant_test",
      "idempotencyKey": "test_'$i'"
    }'
  sleep 1
done

# 4. Check circuit breaker health
curl http://localhost:8082/actuator/health

# 5. Restart fraud service
docker-compose start fraud-service

# 6. Circuit breaker will recover automatically after waitDurationInOpenState
```

### Expected Behavior

- **Calls 1-5**: Fail as fraud service is unavailable
- **Call 6+**: Circuit breaker opens (OPEN state)
- **All subsequent calls**: Fast-fail without contacting fraud service
- **After 5 seconds**: Circuit enters HALF_OPEN state
- **Next call**: Tests if fraud service recovered
- **Service recovered**: Circuit transitions to CLOSED

## Production Recommendations

### 1. Alert Setup

```
Alert when:
- Circuit breaker state = OPEN
- Failure rate > 25%
- Response time > 3 seconds
```

### 2. Configuration Tuning

```yaml
# For stable services
slidingWindowSize: 20
minimumNumberOfCalls: 10
waitDurationInOpenState: 30s

# For volatile services
slidingWindowSize: 5
minimumNumberOfCalls: 3
waitDurationInOpenState: 5s
```

### 3. Monitoring Tools

- Spring Boot Actuator for health checks
- Prometheus metrics export
- Grafana dashboards for visualization
- ELK stack for log aggregation

### 4. Disaster Recovery

```
If circuit remains OPEN:
1. Check service logs
2. Verify infrastructure
3. Manual circuit reset (if available)
4. Scale up resources
5. Consider traffic shifting
```

## Error Handling Checklist

- ✅ All public endpoints have error handling
- ✅ Circuit breakers on async operations
- ✅ Retry logic with exponential backoff
- ✅ Timeout protection on all calls
- ✅ Validation before processing
- ✅ Meaningful error codes and messages
- ✅ Logging at ERROR and WARN levels
- ✅ Graceful degradation on failures
- ✅ Idempotency for safe retries
- ✅ Health check endpoints

## References

- Resilience4j: https://resilience4j.readme.io/
- Circuit Breaker Pattern: https://martinfowler.com/bliki/CircuitBreaker.html
- Spring Boot Error Handling: https://spring.io/guides/gs/handling-form-submission/

---

**Last Updated**: May 24, 2025
