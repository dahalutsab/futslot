import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'core/session.dart';
import 'features/book/book_screen.dart';
import 'features/bookings/booking_screen.dart';
import 'features/bookings/bookings_screen.dart';
import 'features/deposit/deposit_screen.dart';
import 'features/explore/explore_screen.dart';
import 'features/favourites/favourites_screen.dart';
import 'features/login/login_screen.dart';
import 'features/me/me_screen.dart';
import 'features/messages/messages_screen.dart';
import 'features/messages/thread_screen.dart';
import 'features/notifications/notifications_screen.dart';
import 'features/reviews/reviews_screen.dart';
import 'features/venue/venue_screen.dart';
import 'features/waitlist/waitlist_screen.dart';
import 'layout/shell.dart';

/// Browsing never needs an account; booking, bookings and the account do. Signing in returns to [next].
bool _needsSignIn(String path) =>
    path.startsWith('/bookings') ||
    path.startsWith('/me') ||
    path.startsWith('/messages') ||
    path.startsWith('/notifications') ||
    RegExp(r'^/venues/[^/]+/book').hasMatch(path);

/// Screens that cover the bottom bar (sign-in, a conversation, notifications) open on this navigator.
final _root = GlobalKey<NavigatorState>(debugLabel: 'root');

/// Only paths inside the app, so a crafted link can't send the player elsewhere.
String? safeNext(String? next) => next != null && next.startsWith('/') && !next.startsWith('//') ? next : null;

final routerProvider = Provider<GoRouter>((ref) {
  final signedIn = ValueNotifier(ref.read(sessionProvider) != null);
  late final GoRouter router;
  ref.listen(sessionProvider, (previous, session) {
    signedIn.value = session != null;
    // Redirects don't run again for pushed screens, so a session that ran out has to leave them explicitly
    if (previous != null && session == null && ref.read(sessionProvider.notifier).expired) {
      final here = router.state.uri.toString();
      router.go(Uri(path: '/login', queryParameters: {'next': here}).toString());
    }
  });
  router = GoRouter(
    navigatorKey: _root,
    refreshListenable: signedIn,
    redirect: (context, state) {
      final path = state.uri.path;
      if (path == '/login') {
        return signedIn.value ? safeNext(state.uri.queryParameters['next']) ?? '/' : null;
      }
      if (!signedIn.value && _needsSignIn(path)) {
        return Uri(path: '/login', queryParameters: {'next': state.uri.toString()}).toString();
      }
      return null;
    },
    routes: [
      GoRoute(
        path: '/login',
        builder: (context, state) => LoginScreen(next: safeNext(state.uri.queryParameters['next'])),
      ),
      GoRoute(path: '/notifications', builder: (context, state) => const NotificationsScreen()),
      StatefulShellRoute.indexedStack(
        builder: (context, state, shell) => Shell(shell: shell),
        branches: [
          StatefulShellBranch(routes: [
            GoRoute(
              path: '/',
              builder: (context, state) => const ExploreScreen(),
              routes: [
                GoRoute(
                  path: 'venues/:slug',
                  builder: (context, state) =>
                      VenueScreen(slug: state.pathParameters['slug']!, date: state.uri.queryParameters['date']),
                  routes: [
                    GoRoute(
                      path: 'book',
                      builder: (context, state) => BookScreen(
                        slug: state.pathParameters['slug']!,
                        courtId: state.uri.queryParameters['court'] ?? '',
                        startsAt: state.uri.queryParameters['start'] ?? '',
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ]),
          StatefulShellBranch(routes: [
            GoRoute(
              path: '/bookings',
              builder: (context, state) => const BookingsScreen(),
              routes: [
                GoRoute(
                  path: ':id',
                  builder: (context, state) => BookingScreen(
                    id: state.pathParameters['id']!,
                    justBooked: state.uri.queryParameters.containsKey('booked'),
                    toReview: state.uri.queryParameters.containsKey('review'),
                  ),
                ),
              ],
            ),
          ]),
          StatefulShellBranch(routes: [
            GoRoute(
              path: '/messages',
              builder: (context, state) => const MessagesScreen(),
              routes: [
                // Writing to a venue from its page or a booking: ?venue=<id>&name=<venue>&booking=<id>
                GoRoute(
                  path: 'new',
                  parentNavigatorKey: _root,
                  builder: (context, state) => ThreadScreen(
                    venueId: state.uri.queryParameters['venue'],
                    venueName: state.uri.queryParameters['name'],
                    bookingId: state.uri.queryParameters['booking'],
                  ),
                ),
                GoRoute(
                  path: ':id',
                  parentNavigatorKey: _root,
                  builder: (context, state) => ThreadScreen(conversationId: state.pathParameters['id']),
                ),
              ],
            ),
          ]),
          StatefulShellBranch(routes: [
            GoRoute(
              path: '/me',
              builder: (context, state) => const MeScreen(),
              routes: [
                GoRoute(
                  path: 'deposit',
                  builder: (context, state) => DepositScreen(back: safeNext(state.uri.queryParameters['back'])),
                ),
                GoRoute(path: 'favourites', builder: (context, state) => const FavouritesScreen()),
                GoRoute(path: 'reviews', builder: (context, state) => const ReviewsScreen()),
                GoRoute(path: 'waitlist', builder: (context, state) => const WaitlistScreen()),
              ],
            ),
          ]),
        ],
      ),
    ],
  );
  ref.onDispose(() {
    router.dispose();
    signedIn.dispose();
  });
  return router;
});
