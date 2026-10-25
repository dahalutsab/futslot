# Handoff: FutSlot "Broadsheet" redesign (landing + player home)

## How to use this with Claude Code
1. Unzip this folder into the repo root as `design_handoff_broadsheet/`.
2. Copy `DESIGN.md` to `docs/design/broadsheet.md`, and add this line to the repo's `CLAUDE.md`:
   `All UI work follows docs/design/broadsheet.md. Reference mockups: design_handoff_broadsheet/reference/.`
3. Prompt Claude Code, e.g.:
   - "Read design_handoff_broadsheet/README.md and DESIGN.md. Step 1: swap the player app's tokens in player/src/styles.css to Broadsheet (keep the token names). Don't touch components yet."
   - "Now rebuild backend templates/futslot/home.html + futslot.css to match screen 1a."
   - "Add a new /home route in the player app matching screen 1b (desktop) / 1c (phone)."
   - Later: "Redesign features/venue/venue.html following DESIGN.md."

## About the design files
`reference/FutSlot Redesign.dc.html` is a **design reference built in HTML** — open it in a browser to see the intended look. It is not production code. Recreate it in the existing stacks: Angular 20 + Tailwind v4 (`player/`, `dashboard/`, `admin/`), Thymeleaf (`backend/src/main/resources/templates/futslot/`), and Flutter (`mobile/`). Keep the existing i18n (en/ne), routing, API and booking logic.

## Fidelity
**High-fidelity.** Colors, type, spacing and layout are final. Data (times, prices, player name) is illustrative.

## Screens

### 1a — Landing (server-rendered `futslot/home.html`), designed at 1280px
Page gutter 72px; content left-aligned, no boxes, sections separated by whitespace (64–72px).
- **Nav**: brand "FutSlot" (serif 600, 24px), links Explore / Tournaments / For venues (15px, text color), "EN · नेपाली" toggle (13px, uppercase-tracked 0.08em, 70% ink), Sign in (`.btn-secondary`).
- **Hero**: grid 6fr / 5fr, gap 64px, padding 72px 0 64px, items aligned to bottom.
  - H1 84px / 0.98, letter-spacing -0.025em, two lines: "Book a futsal court." (600) / "Tonight, if you like." (italic 400). Margin-left -0.035em (optical alignment).
  - Intro 19px / 30px, max 50ch, 82% ink (`dir.home.intro`).
  - Search: input (min-height 44px, 16px) + `.btn-primary` "Search", max-width 520px, gap 10px.
  - City links: "Futsal in Kathmandu 10" etc., 15px, accent-700, count in 60% ink, gap 28px.
  - Right: venue photo in `.cmyk` print treatment, aspect 4/5, caption 13px 70% ink.
- **Front-page rail**: thick-thin rule (2px top + 1px bottom, 5px tall, full ink) → dateline row (13px, uppercase, 0.08em tracking, 70% ink, space-between: date · area · venue count · "Games from Rs X") → 1px ink rule.
- **Free next** (`dir.board.title`): H2 30px + "All venues →". Two-column list (column gap 72px), each row: time (serif 600, 19px, tabular) · venue + ", area" (area 62% ink) · dotted leader (1px dotted 45% ink, flex:1) · price. Row padding 10px 0, 16px/28px. Hover → accent-700. Closed by 1px ink rule.
- **Venues** (`dir.allVenues`): kicker label (13px uppercase); 3-col grid, gap 56px 48px. Item: `.halftone` photo 3/2 → area · courts kicker → name H3 24/28 → headline italic 16/26 78% ink → "From Rs X" + "Free today HH:MM" (accent-700). No card box.
- **How booking works**: H2 40/44; 3 columns gap 56px; each with `.cmyk-num` numeral (96px) then H3 24px + body 16/27, max 34ch.
- **FAQ**: grid 4fr/7fr — H2 left, Q&A list right (Q 21px 600, A 16/27, gap 36px). Static (current `<details>` behavior can stay).
- **Owners close**: H2 56/60 "Run a *futsal?*" (italic on 2nd word), body 18/29 max 52ch, `.btn-primary` "List your futsal" (44px).
- **Footer**: brand + tagline + maker left; demo disclaimer right in italic, 13px 70% ink.

### 1b — Player home (NEW screen, signed in), desktop 1280px
Suggested route `/home` (make it the signed-in default; `/explore` stays for signed out). Desktop nav adds Home before Explore; Messages unread count is a `.tag-accent`.
- **Rail**: same thick-thin rule + dateline: date · "Good afternoon, {first name}" · "N games this week" · "Deposit Rs X".
- **Lead (grid 7fr/4fr, gap 72px, padding 56px 0 64px)**
  - Left, the next booking as the lead story: kicker "Your next game" + status tag; H1 72/76 "Tonight, 18:00 *at Dhuku.*"; venue/court/time line 19/30.
  - Countdown `.cmyk-num` 132px ("2:14") + "hours to kick-off" (toggleable).
  - Money breakdown as dotted-leader rows (16/28): court, extras, advance paid (accent-700, negative), then a 1px ink rule and bold "Pay at the venue". Below: split status ("7 of 10 paid, Rs 200 each") and the exact current cancellation fee — computed by `shared/booking-model.ts`.
  - Actions: primary "Remind the 3 who owe" (split share), secondary "Directions", ghost "Booking details".
  - Right, **"Needs you"**: H2 28/32 + one-line sub; list rows separated by `--color-divider` hairlines: Phosphor duotone icon (22px; magenta-700 = needs action, accent-700 = in progress, 60% ink = optional) · title (600 16/24) + detail (14/22, 70% ink) · amount right. Sources: unpaid split dues, payments being checked, waitlist, pending reviews, held bookings awaiting payment. Most urgent first.
- **Second row (same grid)**: "This week" `.table` (Day, Venue, Time, Status tag, Total right-aligned, tabular figures) + "All bookings →"; right: "Games looking for players" (matchmaking) rows with `.btn-secondary` Join.
- **Tournament line**: kicker with tournament/group, then one serif sentence 32/40 (second half italic) + "Fixtures and table →".

### 1c — Player home, phone 390px
Same content condensed: header (brand + bell 44px), rail (date · deposit), lead H1 40/44, countdown 72px beside the "left to pay / N of 10 paid" line, two buttons (44px), "Needs you" top 3. Bottom tab bar: Home, Explore, Bookings, Play, Me — 60px tall, Phosphor duotone 24px, label 12px; current tab full ink, others 60% ink; top hairline only.

## Status mapping (replaces the 5-color chips in player-web.md)
| State | Tag |
|---|---|
| Confirmed | `.tag-accent` (cyan) |
| Held / your share due / needs action | `.tag-accent-2` (magenta) |
| Payment being checked, waitlist | `.tag-neutral` |
| Played, cancelled | `.tag-neutral` |
| No-show | `.tag-accent-2` |
Never use both accents in one small component.

## Design tokens
See `broadsheet-styles.css` (`:root`) — it is the source of truth. Key values:
- bg `#f3f2f2`, surface `#eae9e9`, text `#201e1d`, divider = text @16%
- accent (cyan) `#0088b0`, hover 600 `#1186ac`, pressed/text 700 `#006786`
- accent-2 (magenta) `#d6006c`, text 700 `#aa0b56`
- process yellow `#edbb00` (print treatments only, never UI)
- Font: Source Serif 4 (400, 600, italic 400) for everything; Devanagari fallback: keep Noto Serif Devanagari or the existing Mukta for `lang="ne"` text.
- Spacing: 5 / 10 / 15 / 20 / 30 / 40px; radius 1 / 2 / 4px; shadows sm/md/lg in the CSS.
- Muted text = text color at 70% (small) or 78–82% (body).
- Focus: `outline: 2px solid var(--color-accent); outline-offset: 2px`.

### Mapping onto the existing Tailwind tokens (player/src/styles.css)
| Existing | Broadsheet |
|---|---|
| `--bg` | `#f3f2f2` |
| `--surface` | `#f3f2f2` (pages are paper; use `#eae9e9` only for genuine cards) |
| `--surface-2` | `#eae9e9` |
| `--border` | `rgba(32,30,29,.16)` |
| `--text` / `--text-muted` / `--text-subtle` | `#201e1d` / `#4a4746` / `#605d5d` |
| `--ink` (primary button) | `#0088b0`, `--ink-fg` `#f3f2f2` |
| `--brand` | `#201e1d` |
| `--ring` | `#0088b0` |
| `--danger` | `#aa0b56` |
| `--font-sans`, `--font-display` | `"Source Serif 4", serif` |
| turf band | remove — header is paper, not green |
Dark theme: Broadsheet has no dark surfaces; drop dark mode or keep the existing one unchanged until designed.

## Assets
- Venue photos: `reference/assets/v1–v6.jpg`, copied from `backend/media/venues/*`. In production use the real venue cover photos.
- Icons: Phosphor duotone (`@phosphor-icons/web` or `phosphor-flutter`), replacing lucide.
- `.cmyk` / `.cmyk-num` / `.halftone`: CSS in `broadsheet-styles.css`; the `.cmyk` SVG filters are in `reference/_ds/.../_ds_bundle.js` (print-plates). Port those filter defs into the app shell if you use `.cmyk`; otherwise use `.halftone` everywhere.

## Files
- `DESIGN.md` — the rules to put in the repo for all future screens.
- `broadsheet-styles.css` — tokens + component classes (.btn, .tag, .input, .table, .nav, .card, .cmyk, .cmyk-num, .halftone).
- `reference/FutSlot Redesign.dc.html` — the mockups (open in a browser via a local server, e.g. `npx serve reference`).
