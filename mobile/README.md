# FutSlot mobile app

The player app for Android and iOS, in Flutter. It does what the player web app (`../player`) does: signing in,
finding a venue, booking and paying the advance, your bookings (cancelling with the fee shown first), the FutSlot
booking deposit, messages with venues, notifications, saved venues, reviews, the waitlist, memberships, splitting
a booking's cost (equally or baazi), pickup games, teams, tournaments and the player profile. Design notes:
[docs/design/mobile.md](../docs/design/mobile.md).

## Run it

Start the API first (see the root README), then with an emulator or phone connected:

```bash
flutter pub get
flutter run                                   # talks to the API on this computer
flutter run --dart-define=API_URL=https://api.example.com
```

Without `API_URL` the app uses `http://10.0.2.2:8080` on the Android emulator (the computer it runs on) and
`http://localhost:8080` elsewhere. A phone on your Wi-Fi needs `--dart-define=API_URL=http://<computer's IP>:8080`.
Plain http only works in debug builds on Android; release builds need https.

Create an account with any phone number (in development the 6-digit code is printed in the API log,
`SMS to +977...`), then sign in with it and the password. "Continue with Google" shows when the API has a Google
web client id; on Android it also needs an Android OAuth client for the app id and signing key in the same Google
Cloud project.

```bash
flutter test         # unit and widget tests
flutter analyze
flutter build apk    # release APK in build/app/outputs/flutter-apk
```

## How it's put together

- `lib/core`: the API client (`api.dart`, every call the app makes, with the token refresh on 401), API types,
  the session (phone + password sign-in, Google, texted codes to sign up or reset; tokens kept in the keychain /
  keystore), theme tokens.
- `lib/shared`: booking rules in plain words and the cancel-fee preview (`booking_model.dart`, the same rules
  and test cases as the web app's `booking-model.ts`), formatting, the "I paid" form, venue cards, status chips.
- `lib/features`: one folder per screen.
- `lib/router.dart`: the screens and the bottom bar (Explore, Bookings, Play, Messages, Me). Play switches
  between games, tournaments and teams at the top. Browsing venues and tournaments works signed out; booking,
  bookings, games, teams, messages, notifications and the account ask to sign in and come back to the same place
  after.
- `lib/shared/labels.dart`: how API values read to a player (levels, roles, split rules...), the same words as
  the web app's `labels.ts`.

Riverpod holds the app-wide state (session, API client, theme, the unread counts in `core/badges.dart`, checked
every minute and when the app comes back to the front); each screen loads its own data the way the
web app's components do. Not here yet: push notifications (they need the Firebase project).
