# FutSlot player web app

The web app players use until the mobile app is built: find a venue, see its free slots and prices, book
and pay the advance, manage bookings and the FutSlot booking deposit. Angular 21 (standalone components,
signals, OnPush), Tailwind CSS v4, Angular CDK and Lucide icons, on the same design tokens as the owner
dashboard. Design notes: [docs/design/player-web.md](../docs/design/player-web.md).

## Run it

Start the API first (see the root README), then:

```bash
npm install
npm start            # http://localhost:4300, with /api and /media proxied to localhost:8080
```

Browsing venues works signed out. Sign in with any phone number; in development the 6-digit code is printed
in the API log (`SMS to +977...`). The owner dashboard runs on port 4200, so both can run side by side.

```bash
npm run build        # production build in dist/player
npm test             # unit tests (Vitest)
```

## How it's put together

- `src/styles.css`: design tokens, shared with the dashboard. Colour means booking state and is used the same
  way everywhere; use the token classes (`bg-confirmed-bg`, `text-muted`…), never raw colours.
- `src/app/core`: session (phone + code sign-in, token refresh on 401), the API client (`api.ts`, every call
  the app makes), theme, API types.
- `src/app/shared`: booking rules in plain words and the cancel-fee preview (`booking-model.ts`, mirroring the
  server), the "I paid" form used for advances and top-ups, venue cards, status chips.
- `src/app/layout`: the shell (top bar, and a bottom bar on phones).
- `src/app/features`: one folder per screen, lazy-loaded.

In production, serve the built app and the API from the same domain (e.g. `/` for the app and `/api` for
the API behind one reverse proxy), so no CORS setup is needed.
