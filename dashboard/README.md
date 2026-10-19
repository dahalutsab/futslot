# FutSlot dashboard

The web app venue owners and staff use to run their venue: schedule, bookings, payments, customers,
reports and settings. Angular 21 (standalone components, signals, OnPush), Tailwind CSS v4, Angular CDK and
Lucide icons. Design direction: [docs/design/inspirations.md](../docs/design/inspirations.md).

## Run it

Start the API first (see the root README), then:

```bash
npm install
npm start            # http://localhost:4200, with /api and /media proxied to localhost:8080
```

Sign in with a phone number and password, or create an account; in development the 6-digit code for signing up or
resetting a password is printed in the API log (`SMS to +977...`). A number that isn't an owner or staff anywhere lands on "No venue yet".

```bash
npm run build        # production build in dist/dashboard
npm test             # unit tests (Vitest)
```

## How it's put together

- `src/styles.css`: design tokens. Colour means booking state (confirmed, held, to verify, blocked,
  tournament, no-show) and is used the same way everywhere; use the token classes (`bg-confirmed-bg`,
  `text-muted`…), never raw colours. Light and dark themes.
- `src/app/core`: session (phone + password sign-in, Google, texted codes to sign up or reset; token refresh on 401), venue context (the venue being
  managed and the user's permissions there), theme, API types.
- `src/app/layout`: the shell (sidebar on wide screens, top and bottom bars on phones) and the navigation
  list, filtered by permission.
- `src/app/features`: one folder per screen, lazy-loaded.

In production, serve the built app and the API from the same domain (e.g. `/` for the dashboard and
`/api` for the API behind one reverse proxy), so no CORS setup is needed.
