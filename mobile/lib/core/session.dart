import 'dart:async';
import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import 'api_types.dart';
import 'config.dart';

class StoredSession {
  const StoredSession({required this.accessToken, required this.refreshToken, required this.user});

  final String accessToken;
  final String refreshToken;
  final User user;

  factory StoredSession.fromJson(Json json) => StoredSession(
        accessToken: json['accessToken'] as String,
        refreshToken: json['refreshToken'] as String,
        user: User.fromJson(json['user'] as Json),
      );

  Json toJson() => {'accessToken': accessToken, 'refreshToken': refreshToken, 'user': user.toJson()};
}

/// Keeps the tokens in the phone's keychain / keystore, so the app stays signed in.
class SessionStore {
  SessionStore([FlutterSecureStorage? storage]) : _storage = storage ?? const FlutterSecureStorage();

  static const _key = 'futslot.player.session';

  final FlutterSecureStorage _storage;

  Future<StoredSession?> read() async {
    try {
      final raw = await _storage.read(key: _key);
      return raw == null ? null : StoredSession.fromJson(jsonDecode(raw) as Json);
    } catch (_) {
      return null;
    }
  }

  Future<void> write(StoredSession? session) async {
    try {
      if (session == null) {
        await _storage.delete(key: _key);
      } else {
        await _storage.write(key: _key, value: jsonEncode(session.toJson()));
      }
    } catch (_) {
      // Stay signed in until the app closes
    }
  }
}

final sessionStoreProvider = Provider<SessionStore>((ref) => SessionStore());

/// What was stored when the app started; main() overrides it.
final initialSessionProvider = Provider<StoredSession?>((ref) => null);

/// For the sign-in calls, which don't carry a token.
final authHttpProvider = Provider<Dio>((ref) => Dio(BaseOptions(
      baseUrl: apiBaseUrl,
      connectTimeout: const Duration(seconds: 15),
      receiveTimeout: const Duration(seconds: 30),
    )));

final sessionProvider = NotifierProvider<Session, StoredSession?>(Session.new);

/// Who is signed in. Login is by phone and a one-time code. The access token lasts 15 minutes and is refreshed
/// on demand with the refresh token.
class Session extends Notifier<StoredSession?> {
  Future<void>? _refreshing;

  bool _expired = false;

  @override
  StoredSession? build() => ref.read(initialSessionProvider);

  Dio get _http => ref.read(authHttpProvider);

  bool get signedIn => state != null;

  User? get user => state?.user;

  String? get accessToken => state?.accessToken;

  Future<OtpSent> requestCode(String phone) async {
    final response = await _http.post<Json>('/api/v1/auth/otp/request', data: {'phone': phone});
    return OtpSent.fromJson(response.data!);
  }

  /// [fullName] is only needed when this phone number hasn't been used before.
  Future<User> verify(String phone, String code, [String? fullName]) async {
    final response = await _http.post<Json>('/api/v1/auth/otp/verify', data: {
      'phone': phone,
      'code': code,
      if (fullName != null && fullName.isNotEmpty) 'fullName': fullName,
    });
    final tokens = TokenResponse.fromJson(response.data!);
    _store(tokens);
    return tokens.user;
  }

  /// Swaps the refresh token for a new pair; concurrent callers share one request. Signs out when the refresh
  /// token is no longer accepted.
  Future<void> refresh() {
    final refreshToken = state?.refreshToken;
    if (refreshToken == null) {
      return Future.error(StateError('Not signed in'));
    }
    return _refreshing ??= () async {
      try {
        final response = await _http.post<Json>('/api/v1/auth/refresh', data: {'refreshToken': refreshToken});
        _store(TokenResponse.fromJson(response.data!));
      } on DioException catch (error) {
        final status = error.response?.statusCode;
        if (status != null && status >= 400 && status < 500) {
          _expired = true;
          _persist(null);
        }
        rethrow;
      } finally {
        _refreshing = null;
      }
    }();
  }

  Future<void> signOut() async {
    final refreshToken = state?.refreshToken;
    _persist(null);
    if (refreshToken != null) {
      // Best effort: the session is gone locally either way
      unawaited(_http.post<void>('/api/v1/auth/logout', data: {'refreshToken': refreshToken}).then((_) {}, onError: (_) {}));
    }
  }

  /// The player changed their name or details.
  void updateUser(User user) {
    final current = state;
    if (current != null) {
      _persist(StoredSession(accessToken: current.accessToken, refreshToken: current.refreshToken, user: user));
    }
  }

  /// The last session ended because it ran out (not because the player signed out).
  bool get expired => _expired;

  /// [expired], once: the sign-in screen says so.
  bool takeExpired() {
    final expired = _expired;
    _expired = false;
    return expired;
  }

  void _store(TokenResponse tokens) {
    _expired = false;
    _persist(StoredSession(accessToken: tokens.accessToken, refreshToken: tokens.refreshToken, user: tokens.user));
  }

  void _persist(StoredSession? session) {
    state = session;
    unawaited(ref.read(sessionStoreProvider).write(session));
  }
}
