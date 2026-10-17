# Player web app: design direction

For players booking futsal in Nepal, mostly on a phone, often on mobile data. It shares the owner dashboard's
tokens and rules (see [inspirations.md](inspirations.md)): calm neutral surfaces, ink buttons, colour only
for meaning, Inter with Noto Sans Devanagari, 44px touch targets, light and dark themes.

## What's different for players

- **Phone first.** A bottom bar (Explore, Bookings, Me) on phones and a top bar on wide screens. Screens are
  one column, at most `max-w-xl` for tasks (booking, paying) and wider only for browsing.
- **Browsing never needs an account.** Venues, slots, prices, rules and reviews are public; signing in is
  asked for at "Book", and the player comes back to the same slot after.
- **Money is always spelled out before it's committed.** The quote shows court, extras, discount, total,
  what to pay now and what's left for the venue. Cancelling shows the exact fee first, worked out the way
  the server does (`shared/booking-model.ts`), including any part taken from the booking deposit.
- **Rules in plain words, second person.** "Cancel 24 hours or more before the game for free", not policy
  fields.
- **Paying is reporting.** Until a gateway is connected players pay the venue's (or FutSlot's) wallet and
  report the transaction ID. One form does this everywhere (`shared/pay-form.ts`), with where to send the
  money shown above it.

## Booking states (same colours as the dashboard)

| State | Chip |
|---|---|
| Held, waiting for payment | Amber, clock |
| Payment being checked | Violet, wallet |
| Confirmed | Green, check |
| Played | Muted |
| No-show | Red, flag |
| Cancelled / not paid in time | Muted |
