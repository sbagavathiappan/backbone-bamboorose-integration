# Backbone PLM to Bamboo Rose Webhook Service

Production-ready Java-based webhook service for quote synchronization between **Backbone PLM** and **Bamboo Rose**.

## Architecture

```
Backbone PLM --[webhook]--> Webhook Service --[transformed quote]--> Bamboo Rose
                                 |
                            [H2/PostgreSQL]
                            [async processing]
                            [retry + circuit breaker]
```

## Features

- **HMAC-SHA256 signature validation** for secure webhook reception
- **Async processing** with configurable thread pool
- **Automatic retry** with exponential backoff (Resilience4j)
- **Circuit breaker** pattern for Bamboo Rose API calls
- **Event persistence** for audit trail and recovery
- **Prometheus metrics** for monitoring
- **Spring Boot Actuator** health checks
- **Scheduled retry processor** for failed events
- **REST management API** for event monitoring
- **OpenAPI/Swagger UI** for interactive API documentation
- **Dead letter queue** with retry and discard operations

## Tech Stack

- Java 17
- Spring Boot 3.2.5
- Spring Security
- Spring Data JPA
- Resilience4j (retry, circuit breaker)
- H2 (dev) / PostgreSQL (prod)
- Micrometer + Prometheus
- Lombok
- Gradle 8.7

## Quick Start

### Prerequisites

- Java 17+
- (Optional) Gradle 8.7+ — wrapper included

### Build

```bash
# Using Gradle wrapper
./gradlew clean build -x test        # Linux/macOS
gradlew.bat clean build -x test      # Windows
```

### Run

```bash
# Development (H2 in-memory)
./gradlew bootRun          # Linux/macOS
gradlew.bat bootRun        # Windows

# Production with PostgreSQL
SPRING_PROFILES_ACTIVE=prod \
WEBHOOK_SECRET=your-secret \
BACKBONE_PLM_URL=https://api.backboneplm.com \
BACKBONE_API_KEY=your-key \
BAMBOO_ROSE_URL=https://api.bamboorose.com \
BAMBOO_ROSE_API_KEY=your-key \
BAMBOO_ROSE_API_SECRET=your-secret \
./gradlew bootRun
```

### Docker

```bash
docker build -t backbone-bamboorose-integration .
docker run -p 8080:8080 \
  -e WEBHOOK_SECRET=your-secret \
  -e BACKBONE_API_KEY=your-key \
  -e BAMBOO_ROSE_API_KEY=your-key \
  backbone-bamboorose-integration
```

## API Endpoints

### Webhook Endpoints (Public)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/webhook/backbone/quotes` | Receive Backbone quote webhooks |
| POST | `/api/webhook/backbone/quotes/sync` | Trigger quote sync |

### Management Endpoints (Authenticated)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/internal/events` | List all webhook events (paginated) |
| GET | `/api/internal/events/{eventId}` | Get event by ID |
| GET | `/api/internal/events/status/{status}` | Filter by status |
| GET | `/api/internal/events/quote/{quoteId}` | Get events by quote ID |
| GET | `/api/internal/events/recent` | Recent 10 events |
| GET | `/api/internal/stats` | Event statistics |
| GET | `/api/internal/dlt` | List dead letter queue events |
| POST | `/api/internal/dlt/{eventId}/retry` | Retry a specific failed event |
| POST | `/api/internal/dlt/retry-all` | Retry all failed events |
| DELETE | `/api/internal/dlt/{eventId}` | Discard a failed event |
| POST | `/api/internal/health/external` | Check external API health |

### OpenAPI / Swagger

Interactive API documentation is available at:

- **Swagger UI**: `http://localhost:8080/api/swagger-ui/index.html`
- **OpenAPI JSON**: `http://localhost:8080/api/v3/api-docs`

### Health & Metrics

| Path | Description |
|------|-------------|
| `/api/actuator/health` | Health check |
| `/api/actuator/prometheus` | Prometheus metrics |

## Webhook Integration

### Receiving from Backbone PLM

Configure Backbone PLM to send webhooks to:
```
POST https://your-service/api/webhook/backbone/quotes
Headers:
  X-Webhook-Signature: <HMAC-SHA256 signature>
  X-Webhook-Event-Type: QUOTE_CREATED | QUOTE_UPDATED | QUOTE_APPROVED
Body: <quote JSON payload>
```

### Signature Generation

Backbone PLM should sign payloads using HMAC-SHA256 with the shared secret:

```python
import hmac, hashlib
signature = hmac.new(secret.encode(), payload.encode(), hashlib.sha256).hexdigest()
```

### Example Webhook Payload

```json
{
  "id": "Q-12345",
  "quote_number": "QT-2024-001",
  "status": "CREATED",
  "supplier_name": "ABC Manufacturing",
  "currency": "USD",
  "total_amount": 15000.00,
  "created_at": "2024-01-15T10:30:00Z",
  "valid_until": "2024-02-15T10:30:00Z",
  "line_items": [
    {
      "id": "LI-001",
      "product_name": "Cotton T-Shirt",
      "sku": "TSHIRT-001",
      "quantity": 500,
      "unit_price": 12.50,
      "total_price": 6250.00,
      "moq": 100,
      "lead_time_days": 21,
      "color": "Navy",
      "size": "M",
      "material": "100% Cotton",
      "category": "Apparel"
    }
  ],
  "notes": "Rush order",
  "terms": "Net 30"
}
```

## Configuration

### Environment Variables

Copy `.env.example` to `.env` and configure your values:

```bash
cp .env.example .env
```

| Variable | Description | Default |
|----------|-------------|---------|
| `WEBHOOK_SECRET` | HMAC signing secret | `change-this-in-production` |
| `BACKBONE_PLM_URL` | Backbone PLM API base URL | `https://api.backboneplm.com` |
| `BACKBONE_API_KEY` | Backbone PLM API key | - |
| `BAMBOO_ROSE_URL` | Bamboo Rose API base URL | `https://api.bamboorose.com` |
| `BAMBOO_ROSE_API_KEY` | Bamboo Rose API key | - |
| `BAMBOO_ROSE_API_SECRET` | Bamboo Rose API secret | - |

### Production Database (PostgreSQL)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/webhookdb
    driver-class-name: org.postgresql.Driver
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

## Retry & Resilience

- **Max retries**: 3 attempts per event
- **Backoff**: Exponential (10s, 20s, 40s)
- **Circuit breaker**: Opens after 50% failure rate over 10 calls
- **Timeout**: 10s per Bamboo Rose API call
- **Scheduled processor**: Runs every 60s to pick up retriable events
- **Dead letter queue**: Permanently failed events are moved to DLT for manual review via `/api/internal/dlt`

## Monitoring

### Metrics (Prometheus)

- `webhook_sync_total` - Total webhook syncs by event type
- `webhook_sync_success` - Successful syncs
- `webhook_sync_failure` - Failed syncs by error type
- `webhook_sync_duration` - Sync duration histogram

### Database Console (Dev Only)

Access H2 console at: `http://localhost:8080/api/h2-console`
- JDBC URL: `jdbc:h2:mem:webhookdb`
- Username: `sa`
- Password: (blank)

## Testing

```bash
# Unit tests
./gradlew test

# Full build with tests
./gradlew build

# With coverage (add Jacoco plugin to build.gradle.kts first)
./gradlew test jacocoTestReport
```

## Project Structure

```
src/main/java/com/backbonebamboorose/
├── config/          # Async, Security, OpenAPI, RestTemplate, Properties
├── controller/      # Webhook & Management endpoints
├── dto/             # Request/Response DTOs
├── exception/       # Custom exceptions & global handler
├── model/           # JPA entities & domain models
│   ├── backbone/    # Backbone PLM quote model
│   └── bamboorose/  # Bamboo Rose quote model
├── repository/      # Spring Data JPA repositories
├── service/         # Business logic services (incl. DeadLetterQueueService)
└── transformer/     # Quote data transformation
```

## License

Proprietary
