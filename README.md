# BKBN to Bamboo Rose Integration Service

Production-ready Java-based webhook service for synchronizing BKBN visual material events with **Bamboo Rose**.

## Architecture

```
BKBN --[VISUALS_READY webhook]--> Webhook Service --[fetch materials]--> BKBN Materials API
                                        |
                                        |--[transform]--> Bamboo Rose
                                        |
                                   [H2/PostgreSQL]
                                   [async processing]
                                   [retry + circuit breaker]
```

## Features

- **BKBN webhook authentication** (Bearer token validation)
- **Automatic material fetching** from BKBN Materials API on webhook receipt
- **Async processing** with configurable thread pool
- **Automatic retry** with exponential backoff (Resilience4j)
- **Circuit breaker** pattern for external API calls
- **Event persistence** for audit trail and recovery
- **Prometheus metrics** for monitoring
- **Spring Boot Actuator** health checks
- **Scheduled retry processor** for failed events
- **Dead letter queue** with retry and discard operations
- **OpenAPI/Swagger UI** for interactive API documentation

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
- OpenAPI/SpringDoc

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
WEBHOOK_SECRET=your-token \
BKBN_CLIENT_ID=your-client-id \
BKBN_CLIENT_SECRET=your-client-secret \
BAMBOO_ROSE_API_KEY=your-key \
./gradlew bootRun
```

### Docker

```bash
docker build -t bkbn-bamboorose-integration .
docker run -p 8080:8080 \
  -e WEBHOOK_SECRET=your-token \
  -e BKBN_CLIENT_ID=your-client-id \
  -e BKBN_CLIENT_SECRET=your-client-secret \
  -e BAMBOO_ROSE_API_KEY=your-key \
  bkbn-bamboorose-integration
```

## API Endpoints

### Webhook Endpoints (Public)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/webhook/bkbn/visuals` | Receive BKBN VISUALS_READY webhooks |

### BKBN Webhook Payload

```json
{
  "event": "VISUALS_READY",
  "timestamp": "2024-01-15T10:30:00Z",
  "orderId": "12345",
  "assignmentId": "12345-A",
  "visualType": "POST",
  "product": "GROUND_PHOTO",
  "realEstatePropertyId": "RE-123"
}
```

### Management Endpoints (Authenticated)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/internal/events` | List all webhook events (paginated) |
| GET | `/api/internal/events/{eventId}` | Get event by ID |
| GET | `/api/internal/events/status/{status}` | Filter by status |
| GET | `/api/internal/events/order/{orderId}` | Get events by order ID |
| GET | `/api/internal/events/type/{eventType}` | Get events by event type |
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

## Configuration

### Environment Variables

Copy `.env.example` to `.env` and configure your values:

```bash
cp .env.example .env
```

| Variable | Description | Default |
|----------|-------------|---------|
| `WEBHOOK_SECRET` | BKBN webhook auth token | `change-this-in-production` |
| `BKBN_BASE_URL` | BKBN Sync API base URL | `https://sync.bkbn.com` |
| `BKBN_CLIENT_ID` | BKBN client ID | - |
| `BKBN_CLIENT_SECRET` | BKBN client secret | - |
| `BAMBOO_ROSE_URL` | Bamboo Rose API base URL | `https://api.bamboorose.com` |
| `BAMBOO_ROSE_API_KEY` | Bamboo Rose API key | - |
| `BAMBOO_ROSE_API_SECRET` | Bamboo Rose API secret | - |

## Processing Flow

1. **Receive webhook**: BKBN sends `VISUALS_READY` event to `/api/webhook/bkbn/visuals`
2. **Validate auth**: Validates the Bearer token against configured `WEBHOOK_SECRET`
3. **Persist event**: Stores the webhook event in the database with PENDING status
4. **Fetch materials**: Calls BKBN Materials API to retrieve deliverables for the order
5. **Transform**: Maps BKBN materials to Bamboo Rose quote format
6. **Sync to Bamboo Rose**: Sends the transformed data to Bamboo Rose API
7. **Update status**: Marks event as COMPLETED or schedules retry on failure

## Retry & Resilience

- **Max retries**: 3 attempts per event
- **Backoff**: Exponential (10s, 20s, 40s)
- **Circuit breaker**: Opens after 50% failure rate over 10 calls
- **Timeout**: 10s per API call
- **Scheduled processor**: Runs every 60s to pick up retriable events
- **Dead letter queue**: Permanently failed events moved to DLT for manual review via `/api/internal/dlt`

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
│   ├── bkbn/        # BKBN webhook event + materials models
│   └── bamboorose/  # Bamboo Rose quote model
├── repository/      # Spring Data JPA repositories
├── service/         # Business logic services (incl. DeadLetterQueueService)
└── transformer/     # BKBN-to-BambooRose data transformation
```

## License

Proprietary
