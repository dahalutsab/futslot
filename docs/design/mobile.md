# Mobile app: design direction

The player web app's rules (see [player-web.md](player-web.md)), on a phone screen. The tokens in
`mobile/lib/core/theme.dart` are the same values as the web apps' `styles.css`, in light and dark: calm neutral
surfaces, ink buttons, colour only for meaning, Inter with Noto Sans Devanagari (bundled, so nothing is
downloaded on mobile data), 44px touch targets.

- **A bottom bar, three tabs.** Explore, Bookings, Me. Each tab keeps its own back stack; tapping the open tab
  again goes back to its first screen. Signing in is a full screen over the tabs.
- **Same words as the web.** Booking rules, the cancel fee, the deposit and payment states read exactly as they
  do on the web, worked out by the same rules (`shared/booking_model.dart`, tested with the web's cases).
- **One column, at most 576px wide for tasks**, so a tablet doesn't stretch a form across the screen.
- **Pull to refresh** on every list and detail screen; failures say what went wrong and offer "Try again".
- **Booking states** use the same chips as the web: amber held, violet being checked, green confirmed, red
  no-show, muted for played, cancelled and not paid in time.
