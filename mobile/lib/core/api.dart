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

  Future<Page<VenueSummary>> venues({String? q, String? city, int page = 0, int size = 20}) => _get(
        '/api/v1/venues',
        (json) => Page.fromJson(json as Json, VenueSummary.fromJson),
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

  Future<Page<Review>> reviews(String slug) => _get(
        '/api/v1/venues/$slug/reviews',
        (json) => Page.fromJson(json as Json, Review.fromJson),
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

  // Memberships

  Future<List<Membership>> memberships() => _get('/api/v1/me/memberships', (json) => _listOf(json, Membership.fromJson));

  // Account

  Future<User> updateMe({required String fullName, String? email, required String preferredLanguage}) async {
    final response = await _http.patch<Json>('/api/v1/me', data: {
      'fullName': fullName,
      'email': ?email,
      'preferredLanguage': preferredLanguage,
    });
    return User.fromJson(response.data!);
  }

  Future<T> _get<T>(String path, T Function(Object? json) read, [Map<String, Object?>? query]) async {
    final response = await _http.get<Object?>(path, queryParameters: query);
    return read(response.data);
  }

  Future<T> _post<T>(String path, Object body, T Function(Object? json) read) async {
    final response = await _http.post<Object?>(path, data: body);
    return read(response.data);
  }

  static List<T> _listOf<T>(Object? json, T Function(Json) from) =>
      (json as List<dynamic>).map((item) => from(item as Json)).toList();
}
