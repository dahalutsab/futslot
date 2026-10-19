import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'api_types.dart';
import 'config.dart';
import 'session.dart';

/// Adds the access token to API calls, and on a 401 refreshes it once and retries.
class AuthInterceptor extends Interceptor {
  AuthInterceptor(this._dio, this._session);

  final Dio _dio;

  final Session Function() _session;

  static bool _public(RequestOptions options) => options.path.startsWith('/api/v1/auth/');

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    final token = _session().accessToken;
    if (token != null && !_public(options)) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    handler.next(options);
  }

  @override
  Future<void> onError(DioException err, ErrorInterceptorHandler handler) async {
    final options = err.requestOptions;
    final session = _session();
    if (err.response?.statusCode != 401 || options.extra['retried'] == true || _public(options) || !session.signedIn) {
      return handler.next(err);
    }
    try {
      await session.refresh();
    } catch (_) {
      return handler.next(err);
    }
    try {
      options.extra['retried'] = true;
      options.headers['Authorization'] = 'Bearer ${session.accessToken}';
      handler.resolve(await _dio.fetch<dynamic>(options));
    } on DioException catch (retryError) {
      handler.next(retryError);
    }
  }
}

final apiHttpProvider = Provider<Dio>((ref) {
  final dio = Dio(BaseOptions(
    baseUrl: apiBaseUrl,
    connectTimeout: const Duration(seconds: 15),
    receiveTimeout: const Duration(seconds: 30),
  ));
  dio.interceptors.add(AuthInterceptor(dio, () => ref.read(sessionProvider.notifier)));
  return dio;
});

final apiProvider = Provider<Api>((ref) => Api(ref.watch(apiHttpProvider)));

/// Every API call the player app makes. Calls under /venues work signed out too.
class Api {
  Api(this._http);

  final Dio _http;

  // Venues

  Future<Paged<VenueSummary>> venues({String? q, String? city, int page = 0, int size = 20}) => _get(
        '/api/v1/venues',
        (json) => Paged.fromJson(json as Json, VenueSummary.fromJson),
        {'q': ?q, 'city': ?city, 'page': page, 'size': size},
      );

  Future<List<VenueSummary>> featured() => _get(
        '/api/v1/featured-venues',
        (json) => _listOf(json, VenueSummary.fromJson),
        {'placement': 'HOME_CAROUSEL'},
      );

  Future<VenueDetail> venue(String slug) => _get('/api/v1/venues/$slug', (json) => VenueDetail.fromJson(json as Json));

  Future<Availability> availability(String slug, String date) =>
      _get('/api/v1/venues/$slug/availability', (json) => Availability.fromJson(json as Json), {'date': date});

  Future<List<AddOnItem>> addOns(String slug) =>
      _get('/api/v1/venues/$slug/add-ons', (json) => _listOf(json, AddOnItem.fromJson));

  Future<Paged<Review>> reviews(String slug) => _get(
        '/api/v1/venues/$slug/reviews',
        (json) => Paged.fromJson(json as Json, Review.fromJson),
        {'page': 0, 'size': 10},
      );

  // Bookings

  Future<Quote> quote(BookingRequest request) =>
      _post('/api/v1/bookings/quote', request.toJson(), (json) => Quote.fromJson(json as Json));

  Future<Booking> book(BookingRequest request) =>
      _post('/api/v1/bookings', request.toJson(), (json) => Booking.fromJson(json as Json));

  /// [scope] is "upcoming" or "past".
  Future<List<Booking>> bookings(String scope, {int page = 0}) => _get(
        '/api/v1/bookings',
        (json) => _listOf(json, Booking.fromJson),
        {'scope': scope, 'page': page, 'size': bookingsPage},
      );

  static const bookingsPage = 20;

  Future<Booking> booking(String id) => _get('/api/v1/bookings/$id', (json) => Booking.fromJson(json as Json));

  Future<Booking> cancel(String id, String reason) => _post(
        '/api/v1/bookings/$id/cancel',
        {if (reason.isNotEmpty) 'reason': reason},
        (json) => Booking.fromJson(json as Json),
      );

  Future<BookingPayments> bookingPayments(String id) =>
      _get('/api/v1/bookings/$id/payments', (json) => BookingPayments.fromJson(json as Json));

  Future<void> payBooking(String id, PaymentClaim claim) => _post('/api/v1/bookings/$id/payments', claim.toJson(), (_) {});

  // Deposit

  Future<Deposit> deposit() => _get('/api/v1/me/deposit', (json) => Deposit.fromJson(json as Json));

  Future<void> topUp(PaymentClaim claim) => _post('/api/v1/me/deposit/top-ups', claim.toJson(), (_) {});

  Future<void> withdraw(PaymentMethod method, String sendTo) =>
      _post('/api/v1/me/deposit/withdrawals', {'method': method.api, 'sendTo': sendTo}, (_) {});

  Future<void> keepDeposit(String withdrawalId) =>
      _post('/api/v1/me/deposit/withdrawals/$withdrawalId/cancel', const <String, dynamic>{}, (_) {});

  // Favourites

  Future<List<VenueSummary>> favourites() => _get('/api/v1/me/favourites', (json) => _listOf(json, VenueSummary.fromJson));

  Future<void> favourite(String venueId, bool on) async {
    final path = '/api/v1/me/favourites/$venueId';
    await (on ? _http.put<void>(path) : _http.delete<void>(path));
  }

  // Reviews

  /// Fails with REVIEW_NOT_FOUND when the booking has no review yet.
  Future<Review> bookingReview(String bookingId) =>
      _get('/api/v1/bookings/$bookingId/review', (json) => Review.fromJson(json as Json));

  Future<Review> writeReview(String bookingId, {required int rating, String? comment, required bool update}) async {
    final path = '/api/v1/bookings/$bookingId/review';
    final body = {'rating': rating, 'comment': ?comment};
    final response = await (update ? _http.put<Json>(path, data: body) : _http.post<Json>(path, data: body));
    return Review.fromJson(response.data!);
  }

  Future<void> deleteReview(String bookingId) => _http.delete<void>('/api/v1/bookings/$bookingId/review');

  Future<Paged<Review>> myReviews() => _get(
        '/api/v1/me/reviews',
        (json) => Paged.fromJson(json as Json, Review.fromJson),
        {'page': 0, 'size': 50},
      );

  /// Played bookings still waiting for a review.
  Future<List<Booking>> toReview() => _get('/api/v1/me/reviews/pending', (json) => _listOf(json, Booking.fromJson));

  // Waitlist

  Future<void> joinWaitlist(String courtId, String startsAt) =>
      _post('/api/v1/waitlist', {'courtId': courtId, 'startsAt': startsAt}, (_) {});

  Future<List<WaitlistEntry>> waitlist() => _get('/api/v1/me/waitlist', (json) => _listOf(json, WaitlistEntry.fromJson));

  Future<void> leaveWaitlist(String entryId) => _http.delete<void>('/api/v1/me/waitlist/$entryId');

  // Notifications

  Future<Paged<AppNotification>> notifications({int page = 0}) => _get(
        '/api/v1/notifications',
        (json) => Paged.fromJson(json as Json, AppNotification.fromJson),
        {'page': page, 'size': 20},
      );

  Future<int> unreadNotifications() =>
      _get('/api/v1/notifications/unread-count', (json) => (json as Json)['unread'] as int);

  Future<void> readNotification(String id) => _post('/api/v1/notifications/$id/read', const <String, dynamic>{}, (_) {});

  Future<void> readAllNotifications() => _post('/api/v1/notifications/read-all', const <String, dynamic>{}, (_) {});

  // Messages

  Future<List<ConversationSummary>> conversations() =>
      _get('/api/v1/me/conversations', (json) => _listOf(json, ConversationSummary.fromJson));

  /// Newest first, [messagesPage] at a time; [before] pages back through older ones.
  Future<List<ChatMessage>> messages(String conversationId, {String? before}) => _get(
        '/api/v1/me/conversations/$conversationId/messages',
        (json) => _listOf(json, ChatMessage.fromJson),
        {'before': ?before},
      );

  static const messagesPage = 50;

  Future<ChatMessage> reply(String conversationId, String body) => _post(
        '/api/v1/me/conversations/$conversationId/messages',
        {'body': body},
        (json) => ChatMessage.fromJson(json as Json),
      );

  /// Starts (or continues) the conversation with a venue, optionally about one of the player's bookings there.
  Future<ChatMessage> messageVenue(String venueId, String body, {String? bookingId}) => _post(
        '/api/v1/venues/$venueId/messages',
        {'body': body, 'bookingId': ?bookingId},
        (json) => ChatMessage.fromJson(json as Json),
      );

  Future<void> readConversation(String conversationId) =>
      _post('/api/v1/me/conversations/$conversationId/read', const <String, dynamic>{}, (_) {});

  // Memberships

  Future<List<Membership>> memberships() => _get('/api/v1/me/memberships', (json) => _listOf(json, Membership.fromJson));

  Future<List<MembershipPlan>> membershipPlans(String slug) =>
      _get('/api/v1/venues/$slug/membership-plans', (json) => _listOf(json, MembershipPlan.fromJson));

  /// [claim] is left out for a free plan.
  Future<void> buyMembership(String planId, PaymentClaim? claim) =>
      _post('/api/v1/me/memberships', {'planId': planId, ...?claim?.toJson()}, (_) {});

  // Splitting a booking's cost

  /// Fails with SPLIT_NOT_FOUND until the booker sets one up.
  Future<CostSplit> split(String bookingId) => _get('/api/v1/bookings/$bookingId/split', _split);

  /// [bookerSide] only when setting one up.
  Future<CostSplit> setUpSplit(
    String bookingId, {
    required SplitRule rule,
    int? losersPercent,
    required double extrasAmount,
    String? extrasNote,
    String? sideAName,
    String? sideBName,
    Side? bookerSide,
  }) =>
      _put('/api/v1/bookings/$bookingId/split', {
        'rule': rule.apiName,
        'losersPercent': ?losersPercent,
        'extrasAmount': extrasAmount,
        'extrasNote': ?extrasNote,
        'sideAName': ?sideAName,
        'sideBName': ?sideBName,
        'bookerSide': ?bookerSide?.apiName,
      }, _split);

  /// An app user by phone, or a guest by name.
  Future<CostSplit> addSplitPlayer(String bookingId, {String? name, String? phone, required Side side}) => _post(
        '/api/v1/bookings/$bookingId/split/players',
        {'name': ?name, 'phone': ?phone, 'side': side.apiName},
        _split,
      );

  Future<CostSplit> moveSplitPlayer(String bookingId, String playerId, Side side) =>
      _put('/api/v1/bookings/$bookingId/split/players/$playerId', {'side': side.apiName}, _split);

  Future<CostSplit> removeSplitPlayer(String bookingId, String playerId) =>
      _delete('/api/v1/bookings/$bookingId/split/players/$playerId', _split);

  /// Null for an equal split, where only the playing matters.
  Future<CostSplit> splitResult(String bookingId, MatchResult? result) =>
      _post('/api/v1/bookings/$bookingId/split/result', {'result': result?.apiName}, _split);

  Future<CostSplit> undoSplitResult(String bookingId) => _delete('/api/v1/bookings/$bookingId/split/result', _split);

  Future<CostSplit> splitPaid(String bookingId, String playerId, bool paid) {
    final path = '/api/v1/bookings/$bookingId/split/players/$playerId/paid';
    return paid ? _post(path, const <String, dynamic>{}, _split) : _delete(path, _split);
  }

  Future<List<OwedShare>> splitDues() => _get('/api/v1/me/split-dues', (json) => _listOf(json, OwedShare.fromJson));

  static CostSplit _split(Object? json) => CostSplit.fromJson(json as Json);

  // Players and teams

  Future<PlayerCard> myProfile() => _get('/api/v1/me/player-profile', (json) => PlayerCard.fromJson(json as Json));

  Future<PlayerCard> saveProfile({
    Position? position,
    SkillLevel? skillLevel,
    PreferredFoot? preferredFoot,
    String? city,
    String? bio,
    required bool isPublic,
  }) =>
      _put('/api/v1/me/player-profile', {
        'position': ?position?.apiName,
        'skillLevel': ?skillLevel?.apiName,
        'preferredFoot': ?preferredFoot?.apiName,
        'city': ?city,
        'bio': ?bio,
        'isPublic': isPublic,
      }, (json) => PlayerCard.fromJson(json as Json));

  Future<Paged<TeamSummary>> teams({String? q}) => _get(
        '/api/v1/teams',
        (json) => Paged.fromJson(json as Json, TeamSummary.fromJson),
        {'q': ?q, 'page': 0, 'size': 20},
      );

  Future<Team> team(String slug) => _get('/api/v1/teams/$slug', _team);

  /// Teams the player is in or invited to.
  Future<List<Team>> myTeams() => _get('/api/v1/me/teams', (json) => _listOf(json, Team.fromJson));

  Future<Team> createTeam({required String name, String? city, SkillLevel? skillLevel}) => _post(
        '/api/v1/teams',
        {'name': name, 'city': ?city, 'skillLevel': ?skillLevel?.apiName, 'isPublic': true},
        _team,
      );

  Future<Team> invite(String teamId, String phone) =>
      _post('/api/v1/teams/$teamId/invitations', {'phone': phone}, _team);

  Future<void> answerInvite(String teamId, bool accept) =>
      _post('/api/v1/teams/$teamId/invitation/${accept ? 'accept' : 'decline'}', const <String, dynamic>{}, (_) {});

  Future<void> leaveTeam(String teamId) => _post('/api/v1/teams/$teamId/leave', const <String, dynamic>{}, (_) {});

  Future<Team> removeMember(String teamId, String userId) => _delete('/api/v1/teams/$teamId/members/$userId', _team);

  Future<Team> changeRole(String teamId, String userId, TeamRole role) =>
      _put('/api/v1/teams/$teamId/members/$userId/role', {'role': role.apiName}, _team);

  static Team _team(Object? json) => Team.fromJson(json as Json);

  // Matchmaking

  /// Open posts, soonest first. [date] is a Nepal date.
  Future<Paged<MatchPost>> matchPosts({MatchPostType? type, String? date}) => _get(
        '/api/v1/match-posts',
        (json) => Paged.fromJson(json as Json, MatchPost.fromJson),
        {'type': ?type?.apiName, 'date': ?date, 'page': 0, 'size': 20},
      );

  Future<MatchPost> matchPost(String id) => _get('/api/v1/match-posts/$id', _matchPost);

  /// Where and when: one of the player's bookings, or a venue (or just a city) and [startsAt].
  Future<MatchPost> createMatchPost({
    required MatchPostType type,
    String? teamId,
    String? bookingId,
    String? venueId,
    String? city,
    String? startsAt,
    int? playersNeeded,
    SkillLevel? skillLevel,
    double? costPerPlayer,
    String? note,
  }) =>
      _post('/api/v1/match-posts', {
        'type': type.apiName,
        'teamId': ?teamId,
        'bookingId': ?bookingId,
        'venueId': ?venueId,
        'city': ?city,
        'startsAt': ?startsAt,
        'playersNeeded': ?playersNeeded,
        'skillLevel': ?skillLevel?.apiName,
        'costPerPlayer': ?costPerPlayer,
        'note': ?note,
      }, _matchPost);

  Future<MatchPost> cancelMatchPost(String id) =>
      _post('/api/v1/match-posts/$id/cancel', const <String, dynamic>{}, _matchPost);

  /// [teamId]: the team asking, to answer a FIND_OPPONENT post.
  Future<MatchPost> askToJoin(String id, {String? message, String? teamId}) =>
      _post('/api/v1/match-posts/$id/requests', {'message': ?message, 'teamId': ?teamId}, _matchPost);

  Future<MatchPost> withdrawRequest(String id) =>
      _post('/api/v1/match-posts/$id/requests/withdraw', const <String, dynamic>{}, _matchPost);

  Future<MatchPost> answerRequest(String id, String requestId, bool accept) => _post(
        '/api/v1/match-posts/$id/requests/$requestId/${accept ? 'accept' : 'decline'}',
        const <String, dynamic>{},
        _matchPost,
      );

  Future<Paged<MatchPost>> myMatchPosts() =>
      _get('/api/v1/me/match-posts', (json) => Paged.fromJson(json as Json, MatchPost.fromJson), {'size': 50});

  Future<Paged<MatchPost>> myMatchRequests() =>
      _get('/api/v1/me/match-requests', (json) => Paged.fromJson(json as Json, MatchPost.fromJson), {'size': 50});

  static MatchPost _matchPost(Object? json) => MatchPost.fromJson(json as Json);

  // Tournaments

  Future<List<TournamentSummary>> tournaments({required bool upcoming}) => _get(
        '/api/v1/tournaments',
        (json) => _listOf(json, TournamentSummary.fromJson),
        {'upcoming': upcoming},
      );

  Future<TournamentDetail> tournament(String slug) => _get('/api/v1/tournaments/$slug', _tournament);

  /// [squad]: jersey number (or null) by player.
  Future<TournamentDetail> register(String tournamentId, String teamId, Map<String, int?> squad) => _post(
        '/api/v1/tournaments/$tournamentId/registrations',
        {
          'teamId': teamId,
          'squad': [
            for (final MapEntry(key: userId, value: number) in squad.entries)
              {'userId': userId, 'jerseyNumber': ?number},
          ],
        },
        _tournament,
      );

  Future<TournamentDetail> withdrawEntry(String tournamentId, String registrationId) => _post(
        '/api/v1/tournaments/$tournamentId/registrations/$registrationId/withdraw',
        const <String, dynamic>{},
        _tournament,
      );

  Future<List<MyTournamentEntry>> myEntries() =>
      _get('/api/v1/me/tournament-entries', (json) => _listOf(json, MyTournamentEntry.fromJson));

  Future<List<Payment>> entryPayments(String registrationId) =>
      _get('/api/v1/tournament-entries/$registrationId/payments', (json) => _listOf(json, Payment.fromJson));

  Future<void> payEntry(String registrationId, PaymentClaim claim) =>
      _post('/api/v1/tournament-entries/$registrationId/payments', claim.toJson(), (_) {});

  static TournamentDetail _tournament(Object? json) => TournamentDetail.fromJson(json as Json);

  // Account

  Future<User> updateMe({required String fullName, String? email, required String preferredLanguage}) async {
    final response = await _http.patch<Json>('/api/v1/me', data: {
      'fullName': fullName,
      'email': ?email,
      'preferredLanguage': preferredLanguage,
    });
    return User.fromJson(response.data!);
  }

  /// Changes (or first sets) the password. Every device is signed out; the new tokens keep this one in.
  Future<TokenResponse> changePassword({String? currentPassword, required String newPassword}) async {
    final response = await _http.put<Json>('/api/v1/me/password', data: {
      'currentPassword': ?currentPassword,
      'newPassword': newPassword,
    });
    return TokenResponse.fromJson(response.data!);
  }

  Future<T> _get<T>(String path, T Function(Object? json) read, [Map<String, Object?>? query]) async {
    final response = await _http.get<Object?>(path, queryParameters: query);
    return read(response.data);
  }

  Future<T> _post<T>(String path, Object body, T Function(Object? json) read) async {
    final response = await _http.post<Object?>(path, data: body);
    return read(response.data);
  }

  Future<T> _put<T>(String path, Object body, T Function(Object? json) read) async {
    final response = await _http.put<Object?>(path, data: body);
    return read(response.data);
  }

  Future<T> _delete<T>(String path, T Function(Object? json) read) async {
    final response = await _http.delete<Object?>(path);
    return read(response.data);
  }

  static List<T> _listOf<T>(Object? json, T Function(Json) from) =>
      (json as List<dynamic>).map((item) => from(item as Json)).toList();
}
