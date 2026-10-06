# FutSlot

Futsal booking platform: players find and book slots, venue owners manage courts, bookings and payments.

## Repository layout

| Path         | What                                         |
|--------------|----------------------------------------------|
| `backend/`   | Spring Boot API (Java 25, PostgreSQL, Flyway) |
| `dashboard/` | Owner web dashboard                          |
| `mobile/`    | Flutter player app                           |
| `docs/api/`  | OpenAPI contract shared by all clients       |
| `docs/db/`   | Database schema and ER diagrams              |
| `docs/design/` | Design tokens and UI guidelines            |

## Local development

Requirements: Java 25, Docker.

```bash
cp .env.example .env          # optional, defaults work
docker compose up -d db       # start PostgreSQL
cd backend && ./mvnw spring-boot:run
```

- Health check: http://localhost:8080/actuator/health
- Swagger UI: http://localhost:8080/swagger-ui.html

Run the API in Docker instead: `docker compose --profile full up --build`

Tests (uses Testcontainers, Docker must be running): `cd backend && ./mvnw verify`
