# FutSlot

Futsal booking platform: players find and book slots, venue owners manage courts, bookings and payments.

## Repository layout

| Path         | What                                         |
|--------------|----------------------------------------------|
| `backend/`   | Spring Boot API (Java 25, PostgreSQL, Flyway) |
| `dashboard/` | Owner web dashboard                          |
| `player/`    | Player web app                               |
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

### Venue photos

Owners and managers upload photos (`POST /api/v1/owner/venues/{id}/photos`, multipart `file`): JPEG, PNG or
WebP up to 5 MB, at most 15 per venue, checked by their content rather than the file name. The first photo is
the cover shown in listings (`coverPhotoUrl`); any photo can be made the cover. Files are stored under
`futslot.media.dir` (`FUTSLOT_MEDIA_DIR`; a Docker volume in `docker-compose.yml`) and served publicly at the
`/media/...` path each photo returns. Moving to cloud storage later only means a new `MediaStorage`.

### Extras (add-ons)

Venues list what they sell or rent with a booking (`/api/v1/owner/venues/{id}/add-ons`): bibs, balls,
water. Players add them when booking, and staff can change them at the counter during or after the game
(`PUT .../bookings/{id}/add-ons`). The booking total is court + extras - discount: memberships and discount
codes only reduce the court price. A booking keeps the price it was charged for an item, even if the
venue changes the price later.

### Chat

Players message a venue (`POST /api/v1/venues/{venueId}/messages`, optionally about one of their bookings
there). Each player and venue share one conversation. Anyone at the venue who handles bookings can read and
reply. The player sees replies as from the venue; the venue sees which staff member wrote. The other side
gets a notification for each message, and both see unread counts. There's no live connection: the apps
refresh when a notification arrives.

### Waitlist

A player can wait for a booked slot (`POST /api/v1/waitlist`, up to 5 at a time). When a booking on it is
cancelled or its payment hold runs out, everyone still waiting is notified at once, and the first to book
gets it. The others stay on the list in case it frees again. Entries lapse when the slot's time passes.

### Splitting the cost (baazi)

Usually one player books and the group plays. The booker can set up a split on the booking
(`PUT /api/v1/bookings/{id}/split`): add the players (app users by phone, friends without the app by
name), put them on two sides, and pick a rule:

| Rule | Who pays |
|---|---|
| `EQUAL` | Everyone the same, whoever wins |
| `LOSERS_PAY_ALL` | Losers pay everything, winners nothing |
| `LOSERS_PAY_PERCENT` | Losers pay `losersPercent` (e.g. 70), winners the rest |
| `LOSERS_PAY_COURT` | Losers pay the court, winners pay the extras (water, drinks) |

A draw always splits everything equally. Before kick-off the split shows what each side would pay
for each result. After the game the booker records the result and each player's share is fixed: whole
rupees, with any odd rupee going to the booker. The booker ticks players off as they pay, and players
with the app see what they owe in `GET /api/v1/me/split-dues`. FutSlot doesn't handle this money; the
booker pays the venue and collects from the others.

### Teams and matchmaking

Players fill in a profile (position, skill level, city), shown to others by short name ("Ram S.") with
the number of games they've played. Anyone can start a team and becomes its captain. The captain and
vice-captains invite players by the phone number they signed up with. Only the captain changes roles,
and the captain has to hand over before leaving. Private teams are visible only to their members.

Match posts are either "need players" (for one of your bookings, a venue or a city) or "find an
opponent" (for a team you lead). Others ask to join; the poster accepts or declines. A post fills when
its places are taken, which declines everyone still waiting, and reopens if an accepted player drops
out before kick-off. Phone numbers are shared only between the poster and accepted players.

### Tournaments

Venues run tournaments (owners and managers, under `/api/v1/owner/venues/{venueId}/tournaments`) in three
formats: `KNOCKOUT` (byes go to the top seeds when the field isn't a power of two), `LEAGUE` (everyone plays
everyone once) and `GROUP_KNOCKOUT` (2, 4 or 8 round-robin groups, then the top two of each cross into a
knockout: A1-B2, B1-A2).

1. **Draft** the tournament, then **open** registration. It appears in `GET /api/v1/tournaments`.
2. **Teams sign up.** A captain or vice-captain registers the team with a squad of current members. Once
   the places are taken, teams join a waitlist, and the next team moves up when someone withdraws or is
   rejected. Entry fees are paid like booking payments: report a wallet payment for staff to approve, or
   staff record cash. Either way the entry is confirmed.
3. **Close** registration and **draw** fixtures from the confirmed teams (in sign-up order, or shuffled).
4. **Schedule** matches on a court. This blocks the court for `matchMinutes`, so nobody can book over it.
5. **Enter results.** Knockout winners move on straight away; a level knockout match needs penalties. A
   no-show is a walkover (3-0). A result can be corrected until the next round's match has been played.
   Tables use points, then goal difference, then goals scored.

Cancelling frees every court booked for unplayed matches. Squad players are told when fixtures are out,
when their matches are scheduled, and if the tournament is cancelled.

### Subscriptions and featured listings (FutSlot's revenue)

Every venue starts on the free plan. Admins define paid plans (`/api/v1/admin/subscription-plans`), and
owners choose one (`PUT /api/v1/owner/venues/{id}/subscription`). There's no gateway yet, so venues pay
FutSlot the same way players pay venues: send money to the account in
`futslot.subscriptions.payment-instructions` (set `FUTSLOT_PAYMENT_INSTRUCTIONS`), report the transaction
id, and an admin approves it (`GET /api/v1/admin/payments/pending`). Admins can also record a payment
taken some other way. These payments aren't a venue's income, so they never appear in venue reports or
payment queues.

- **Paying starts the period** (a month or a year). A renewal invoice goes out 7 days before the period
  ends.
- **Plan changes wait for renewal**, and so does going back to Free.
- **An unpaid renewal** gives 7 days' grace before the venue drops to Free. A first invoice that's never
  paid lapses the same way.
- **Plan limits (courts, staff) aren't enforced at launch**, so every venue gets everything. Set
  `futslot.subscriptions.enforce-limits=true` to switch them on.

Owners can also buy **featured days** (`POST /api/v1/owner/venues/{id}/featured`): the home carousel, the
top of search, or the top of search in their city. Each placement has a price per day and a number of places
(`futslot.subscriptions.featured.*`; the shipped values are placeholders). An unpaid listing holds its place
for 48 hours. The app shows featured venues from `GET /api/v1/featured-venues`.

### Reports

Venue reports live under `/api/v1/owner/venues/{venueId}/reports` and take `from` / `to` local dates (both
included, default the last 30 days, at most 366 days):

- `summary`: bookings by status and source, cancellation and no-show rates, booked value, discounts,
  what's still owed, money collected (by method, including membership sales), refunds paid and owed,
  and occupancy overall and per court.
- `daily`: one row per day for charts.
- `peak-hours`: played bookings by weekday and start hour.
- `top-customers`: most frequent customers and what they spent.
- `bookings.csv`: every booking in the period, for spreadsheets.

Bookings count by start time; money counts by when it was paid or refunded. Reports need the
`VIEW_REPORTS` permission (owners and managers by default). Admins get FutSlot-wide totals from
`GET /api/v1/admin/reports/summary`.

### Audit log

Actions that matter in a dispute are recorded in the same transaction as the change: money received,
rejected or refunded; bookings entered by the venue, moved or cancelled; staff added; venues approved,
rejected or suspended. Each entry records who did it, and when and from which IP. Owners see their venue's log
(`GET /api/v1/owner/venues/{id}/audit-log`). Admins see everything, IP addresses included
(`GET /api/v1/admin/audit-log`).

### Notifications

Booking and venue changes create in-app notifications (`GET /api/v1/notifications`) and push them to the
user's registered devices (`POST /api/v1/me/devices`). Notifications are sent only after the change
commits. Players get a reminder 2 hours before a confirmed booking
(`futslot.notifications.reminders.*`). Until Firebase is configured, pushes are written to the API log
(`Push to N device(s): ...`).

### Owner dashboard

The web dashboard for venue owners and staff lives in `dashboard/` (Angular). With the API running:
`cd dashboard && npm install && npm start`, then open http://localhost:4200. See `dashboard/README.md`.

### Player web app

Players find venues, book and pay, and manage their bookings and booking deposit in `player/` (Angular),
until the mobile app is built. With the API running: `cd player && npm install && npm start`, then open
http://localhost:4300. See `player/README.md`.

### API contract

`docs/api/openapi.yaml` is exported from the running API. After changing endpoints, start the API
and run `curl -s localhost:8080/v3/api-docs.yaml -o docs/api/openapi.yaml`.

Tests (uses Testcontainers, Docker must be running): `cd backend && ./mvnw verify`
