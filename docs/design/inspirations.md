# Owner dashboard: design inspirations and direction

Research for the venue owner/staff dashboard (Angular). Who it's for: owners and counter staff at futsal
venues in Nepal, often on a phone or a small laptop at the front desk, glancing at it between customers.

## What we looked at

| Source | What to take | What to avoid |
|---|---|---|
| [Playtomic Manager](https://playtomic.com/playtomic-manager) (racket clubs) | The schedule is "the heart" of the product: one screen showing how busy each court is today, with bookings, matches, tournaments and classes on the same grid. A separate dashboard for occupancy, peak times and revenue, filtered by court. | Splitting billing and payments across many tabs. Our staff need one "money to check" queue. |
| [Pitchbooking](https://pitchbooking.com/), [EZFacility](https://www.ezfacility.com/blog/best-sports-facility-management-software), [Upper Hand](https://upperhand.com/software/facility-resource-management/), [Anolla](https://anolla.com/en/sports-facility-management-software) | Real-time calendar of every reservation; colour-coded day/week views so utilisation is visible at a glance; conflict detection; drag to move bookings. | Enterprise density (staff rosters, programs) that a 1–3 court futsal doesn't need. |
| [Linear, Stripe, Vercel (2026 SaaS dashboard roundups)](https://adminlte.io/blog/saas-dashboard-design-examples/), [7 SaaS UI trends 2026](https://www.saasui.design/blog/7-saas-ui-design-trends-2026) | Calm, low-noise UI where colour carries meaning; tables before charts; progressive disclosure (detail one click away); a command palette for finding anything; a 256px sidebar collapsing to 64px; 4–6 KPI cards above the fold. | Dark-first: our users work in bright front desks. Light first, dark as an option. |
| [Dribbble: futsal booking concepts](https://dribbble.com/tags/futsal-booking), e.g. [Concept Futsal booking UI (Sagar Shakya)](https://dribbble.com/shots/21128266-Concept-Futsal-booking-UI), Ngeball sports venue dashboard | Big, tappable slot grids; price shown on the slot; turf-green brand identity that reads "football" instantly. | Decorative gradients and illustration-heavy cards that hide the data. |
| Nepali competitors: [YAKPLAYO](https://yakplayo.com/), [Futsmandu](https://www.futsmandu.com/), [Khel](https://www.khel.it/), [Arena](https://arena.apptechnologies.co/), [FutsalBook](https://www.arkarki.com.np/), [Inho Sports](https://inhosports.com/) | Player-side patterns locals already know: filters like "Near me / Today / Evening", prices in रू, eSewa/Khalti everywhere. | They market venue features ("List your venue") but show little of the owner tooling. That's where FutSlot can stand out. |

## Direction

**Calm operations tool, not a marketing site.** Neutral surfaces, ink-black primary buttons, and colour
used only to mean something. This is the Law of Similarity: a colour means the same thing everywhere it
appears, whether on the schedule grid, in lists, on chips or in notifications.

### Colour = booking state

| State | Colour | Also shown as (never colour alone) |
|---|---|---|
| Free slot | White / empty | "+ Book" on hover or tap |
| Confirmed | Green | Check icon, customer name |
| Waiting for payment (held) | Amber | Clock icon + time left |
| Payment to verify | Violet | Wallet icon; counted in the Payments badge |
| Blocked / maintenance | Grey, hatched | Lock icon + reason |
| Tournament match | Sky blue | Trophy icon + "Team A vs Team B" |
| Played (completed) | Muted grey text | — |
| No-show | Red outline | Flag icon |

Brand turf green appears only in the logo, so it doesn't compete with "confirmed".

### Type

- **Inter** for UI, with tabular figures for money, times and counts.
- **Noto Sans Devanagari** as the fallback for Nepali names and text. It matches Inter's weight and
  x-height, so a customer named "राम श्रेष्ठ" doesn't look out of place. Self-hosted, so it works offline and
  on slow connections.
- Scale: 12 / 14 / 16 / 18 / 24 / 32. Body is 14px on desktop and 16px on phones.

### Information architecture (Miller's Law: 7 or fewer per group)

- **Daily (primary):** Schedule (home), Bookings, Payments (badge), Customers, Messages (badge), Reports.
- **Venue:** Profile and hours, Courts and pricing, Add-ons, Photos, Staff, Plan and billing.
- **Grow:** Memberships and codes, Tournaments, Reviews, Featured listing.
- **Phones:** bottom bar with Schedule, Bookings, Payments, Messages, More.

Staff only see what their permissions allow, so a receptionist never sees Reports or Billing.

### Pareto: the 20% used 80% of the time

1. **Schedule** is the home screen: courts as columns, time as rows, a "now" line, today's takings and the
   next booking at the top. Tapping a free slot opens a prefilled walk-in form; tapping a booking opens a
   side panel with Take payment, Extras, Mark played / no-show, Message, Cancel.
   - On phones: one court at a time (a segmented control, no swipe to avoid gesture conflicts) with slots
     as a vertical list.
2. **Payments to verify:** one queue for wallet payments (bookings, memberships, tournament entries),
   approve or reject in one tap each.
3. **Find anything:** a command palette (Ctrl/⌘ K and a search button on phones) by booking reference,
   phone number or name.

Everything else is one click deeper (progressive disclosure).

### Rules we hold ourselves to

- Touch targets at least 44px with 8px gaps, because staff tap fast.
- Text contrast 4.5:1 or better in both themes.
- Visible focus rings and full keyboard use.
- Skeletons for anything slower than 300ms, an empty state with a next step, and errors that say how to
  fix them.
- Tables first, charts only for a trend (the daily bar on Reports).
- Animations 150–250ms and transform/opacity only; respect reduced motion.
- No emoji as icons: Lucide, one stroke width.

## Stack

Angular 21 (standalone components, signals, OnPush, new control flow), Tailwind CSS v4 with design tokens
as CSS variables (light and dark), Angular CDK for overlays, dialogs and a11y, and Lucide icons.
