# FutSlot mobile app

The player app for Android and iOS, in Flutter. It's being built up to everything the player web app
(`../player`) does; this first part covers signing in, finding a venue, booking and paying the advance, your
bookings (cancelling with the fee shown first), and the FutSlot booking deposit. Design notes:
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

Sign in with any phone number; in development the 6-digit code is printed in the API log (`SMS to +977...`).

```bash
flutter test         # unit and widget tests
flutter analyze
flutter build apk    # release APK in build/app/outputs/flutter-apk
```

## How it's put together

- `lib/core`: the API client (`api.dart`, every call the app makes, with the token refresh on 401), API types,
  the session (phone + code sign-in, tokens kept in the keychain / keystore), theme tokens.
- `lib/shared`: booking rules in plain words and the cancel-fee preview (`booking_model.dart`, the same rules
  and test cases as the web app's `booking-model.ts`), formatting, the "I paid" form, venue cards, status chips.
- `lib/features`: one folder per screen.
- `lib/router.dart`: the screens and the bottom bar (Explore, Bookings, Me). Browsing works signed out;
  booking, bookings and the account ask to sign in and come back to the same place after.

Riverpod holds the app-wide state (session, API client, theme); each screen loads its own data the way the
web app's components do. Not here yet: push notifications (they need the Firebase project), and the rest of
the player features (messages, reviews, waitlist, memberships, splits, games, teams, tournaments).
