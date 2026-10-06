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

### Payments

FutSlot does not hold money. Players pay the venue directly, and FutSlot records and verifies it:

- **Advance via the venue's own wallet.** The venue sets payment instructions (its eSewa / Khalti / Fonepay
  number or bank details). The player pays, then submits the transaction id
  (`POST /api/v1/bookings/{id}/payments`). The slot stays held while staff check their wallet and approve
  or reject it. Approving at least the advance confirms the booking; rejecting gives the player the hold
  time again. A transaction id can only be used once per method.
- **Cash and counter payments.** Staff record what they received against a booking.
- **Refunds.** When a paid booking is cancelled or expires, FutSlot works out what is owed from the venue's
  policy (full refund if the player cancels at least `freeCancelHours` ahead, otherwise
  `lateCancelRefundPct`; always full if the venue cancels or the hold expired). The venue pays it back and
  marks it done.

Gateway checkout (eSewa / Khalti merchant APIs) can be added later as another way to create verified payments.

### Memberships and discount codes

Venues sell **membership plans**: a `GAME_PACK` (N games, each booking uses one and costs nothing) or a
`MONTHLY_PASS` (a percentage off every booking, optionally capped at N games), valid for a number of days.
Players buy one by paying the venue and submitting the transaction id (`POST /api/v1/me/memberships`); it
starts when staff approve the payment, alongside booking payments in `/payments/pending`. Staff can also
sell one at the counter. Once a plan has been sold, its games, discount and validity can't change.

Venues create **discount codes** (percent with an optional cap, or a fixed amount) with optional dates,
minimum booking amount, total and per-player limits. Players enter one when booking in the app.

A booking takes either a `membershipId` or a `discountCode`, not both. `POST /api/v1/bookings/quote` shows
the price, discount and advance before booking. If the booking is cancelled or expires, the code's use is
given back, and so is the membership game, unless the player cancelled inside the free-cancellation window.

### Reviews and favourites

Players review a venue once per completed booking, up to 30 days after playing
(`POST /api/v1/bookings/{id}/review`), so every review comes from someone who played there.
`GET /api/v1/me/reviews/pending` lists bookings still waiting for one. Reviews show the reviewer as
first name and last initial. The venue's `averageRating` and `reviewCount` count published reviews only
and are recomputed on every change.

Venues can reply (owners and managers) but cannot hide reviews; admins publish, hide or remove them
(`POST /api/v1/admin/reviews/{id}/status`). A removed review can't be edited or deleted, so it can't be
posted again.

Players save approved venues with `PUT /api/v1/me/favourites/{venueId}` and list them with
`GET /api/v1/me/favourites`.

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
