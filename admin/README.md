# FutSlot admin

The web app FutSlot staff use to run the platform: approve venues, check payments sent to FutSlot (booking
deposit top-ups, subscription invoices, featured listings), send players their deposits back and venues their
monthly deposit-fee payouts, moderate reviews, manage subscription plans, and read the audit log. Angular 21,
Tailwind CSS v4 and Lucide icons, on the same design tokens as the owner dashboard and player web.

## Run it

Start the API first (see the root README), then:

```bash
npm install
npm start            # http://localhost:4400, with /api proxied to localhost:8080
```

Only accounts with the ADMIN role get in; there's no sign-up. Promote a user in the database (see the root
README), then sign in with their number. In development the 6-digit code is printed in the API log.

```bash
npm run build        # production build in dist/admin
npm test             # unit tests (Vitest)
```

## How it's put together

- `src/app/core`: session (phone + code sign-in, admins only), the API client (`api.ts`), the queue of work
  waiting on admins (`queue.ts`, the counts in the navigation), theme, API types.
- `src/app/shared`: labels for statuses and payments, the reason/reference form used by queue actions.
- `src/app/layout`: the shell (sidebar on wide screens, a row of sections on phones).
- `src/app/features`: one folder per screen, lazy-loaded.

Serve it on its own host (e.g. admin.futslot.com) with `/api` going to the API.
