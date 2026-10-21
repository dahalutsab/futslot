# FutSlot player web app

The web app players use until the mobile app is built: find a venue, see its free slots and prices, book
and pay the advance, manage bookings and the FutSlot booking deposit, split a game's cost, message venues,
review them, wait for a booked slot, buy memberships, and play: pickup games, teams and tournaments.
Angular 21 (standalone components,
signals, OnPush), Tailwind CSS v4, Angular CDK and Lucide icons, on the same design tokens as the owner
dashboard. Design notes: [docs/design/player-web.md](../docs/design/player-web.md).

## Run it

Start the API first (see the root README), then:

```bash
npm install
npm start            # http://localhost:4300, with /api and /media proxied to localhost:8080
```

Browsing venues works signed out. Create an account with any phone number (in development the 6-digit code is
printed in the API log, `SMS to +977...`), then sign in with it and the password. Existing accounts without a
password set one with "Forgot password?". The owner dashboard runs on port 4200, so both can run side by side.

```bash
npm run build        # production build in dist/player
npm test             # unit tests (Vitest)
```

## How it's put together

- `src/styles.css`: design tokens, shared with the dashboard. Colour means booking state and is used the same
  way everywhere; use the token classes (`bg-confirmed-bg`, `text-muted`…), never raw colours.
- `src/app/core`: session (phone + password sign-in, Google, texted codes to sign up or reset; token refresh on 401), the API client (`api.ts`, every call
  the app makes), theme, API types.
- `src/app/shared`: booking rules in plain words and the cancel-fee preview (`booking-model.ts`, mirroring the
  server), the "I paid" form used for advances and top-ups, venue cards, status chips.
- `src/app/layout`: the shell (top bar, and a bottom bar on phones).
- `src/app/features`: one folder per screen, lazy-loaded.
- `src/app/i18n`: English and Nepali (see below).

## English and Nepali

Every word the app shows is in `src/app/i18n/en.ts`, with its Nepali under the same key in `ne.ts`; TypeScript
fails the build if a key is missing from either. Wording with a number or a name in it is a small function, so
each language puts it where it reads naturally. Most keys and their Nepali match the mobile app's
(`mobile/lib/l10n/app_*.arb`). Templates read the current words from `l()` (`{{ l().bookThisSlot }}`), a
signal, so the screen redraws as soon as the player switches, from Me or from the top bar before signing in.

In Nepali, numbers use Devanagari digits with lakh grouping (रु १,२०,०००), times say the part of the day
(बेलुका ६ बजे), and dates are Bikram Sambat (सोम, २ कार्तिक, through `nepali-date-converter`), all in
`src/app/shared/format.ts`. Phone numbers and transaction ids stay in Latin digits, to copy and type. The app
starts in the browser's language, remembers the player's choice, keeps the account's `preferredLanguage` in step
so notifications match, and sends `Accept-Language` so the API can answer in it. Venue and court names and other
things people type stay as written; the date and time picker for posting a game is the browser's own.

In production, serve the built app and the API from the same domain (e.g. `/` for the app and `/api` for
the API behind one reverse proxy), so no CORS setup is needed.
