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

### Venue onboarding

Any signed-in user can create a venue (`POST /api/v1/owner/venues`) and becomes its owner. After adding
opening hours, at least one court and pricing for each court, the owner submits it for approval. Venues
appear in the public `GET /api/v1/venues` listing only after an admin approves them.

There is no sign-up path for admins. Promote an existing user directly in the database, then have them
refresh their token:

```sql
INSERT INTO user_roles (user_id, role) SELECT id, 'ADMIN' FROM users WHERE phone = '+9779800000000';
```

### Notifications

Booking and venue changes create in-app notifications (`GET /api/v1/notifications`) and push them to the
user's registered devices (`POST /api/v1/me/devices`). Notifications are sent only after the change
commits. Players get a reminder 2 hours before a confirmed booking
(`futslot.notifications.reminders.*`). Until Firebase is configured, pushes are written to the API log
(`Push to N device(s): ...`).

### API contract

`docs/api/openapi.yaml` is exported from the running API. After changing endpoints, start the API
and run `curl -s localhost:8080/v3/api-docs.yaml -o docs/api/openapi.yaml`.

Tests (uses Testcontainers, Docker must be running): `cd backend && ./mvnw verify`
