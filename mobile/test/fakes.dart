import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:futslot/core/api_types.dart';
import 'package:futslot/core/session.dart';

/// Answers requests from a function instead of the network: (status, JSON body).
class FakeAdapter implements HttpClientAdapter {
  FakeAdapter(this.answer);

  final (int, Object?) Function(RequestOptions request) answer;

  final requests = <RequestOptions>[];

  @override
  Future<ResponseBody> fetch(RequestOptions options, Stream<Uint8List>? requestStream, Future<void>? cancelFuture) async {
    requests.add(options);
    final (status, body) = answer(options);
    return ResponseBody.fromString(jsonEncode(body), status, headers: {
      Headers.contentTypeHeader: [Headers.jsonContentType],
    });
  }

  @override
  void close({bool force = false}) {}
}

Dio fakeDio((int, Object?) Function(RequestOptions request) answer) =>
    Dio(BaseOptions(baseUrl: 'http://api.test'))..httpClientAdapter = FakeAdapter(answer);

/// Keeps nothing, so tests don't need the platform keychain.
class MemoryStore extends SessionStore {
  StoredSession? saved;

  @override
  Future<StoredSession?> read() async => saved;

  @override
  Future<void> write(StoredSession? session) async => saved = session;
}

const user = User(id: 'u1', fullName: 'Ram Shrestha', phone: '+9779800000000', email: null, preferredLanguage: 'en');

StoredSession session(String accessToken) =>
    StoredSession(accessToken: accessToken, refreshToken: 'refresh-$accessToken', user: user);

Json tokens(String accessToken) => {
      'accessToken': accessToken,
      'tokenType': 'Bearer',
      'expiresInSeconds': 900,
      'refreshToken': 'refresh-$accessToken',
      'user': user.toJson(),
    };
