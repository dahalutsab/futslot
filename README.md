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

### Logging in locally

Auth is passwordless: `POST /api/v1/auth/otp/request` with a phone number, then
`POST /api/v1/auth/otp/verify` with the 6-digit code (plus `fullName` the first time).
In development no SMS is sent; the code is printed in the API log (`SMS to +977...`).
Use the returned `accessToken` as `Authorization: Bearer <token>` (valid 15 minutes) and
`POST /api/v1/auth/refresh` with the `refreshToken` to get a new pair.

Set `JWT_SECRET` and `OTP_SECRET` (32+ random bytes each) in every deployed environment.

### API contract

`docs/api/openapi.yaml` is exported from the running API. After changing endpoints, start the API
and run `curl -s localhost:8080/v3/api-docs.yaml -o docs/api/openapi.yaml`.

Tests (uses Testcontainers, Docker must be running): `cd backend && ./mvnw verify`
