import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:futslot/core/api.dart';
import 'package:futslot/core/session.dart';

import 'fakes.dart';

ProviderContainer _container({required Dio auth, required (int, Object?) Function(RequestOptions) api}) {
  final container = ProviderContainer(overrides: [
    sessionStoreProvider.overrideWithValue(MemoryStore()),
    initialSessionProvider.overrideWithValue(session('old')),
    authHttpProvider.overrideWithValue(auth),
  ]);
  container.read(apiHttpProvider).httpClientAdapter = FakeAdapter(api);
  addTearDown(container.dispose);
  return container;
}

/// The bookings list only answers to the current token.
(int, Object?) _bookings(RequestOptions request) =>
    request.headers['Authorization'] == 'Bearer new' ? (200, <Object>[]) : (401, {'status': 401, 'detail': 'Expired'});

void main() {
  test('an expired access token is refreshed once and the call retried', () async {
    var refreshes = 0;
    final container = _container(
      auth: fakeDio((request) {
        refreshes++;
        return (200, tokens('new'));
      }),
      api: _bookings,
    );
    final api = container.read(apiProvider);

    // Two calls at once share one refresh
    final results = await Future.wait([api.bookings('upcoming'), api.bookings('past')]);

    expect(results, [isEmpty, isEmpty]);
    expect(refreshes, 1);
    expect(container.read(sessionProvider)?.accessToken, 'new');
  });

  test('a refresh token that is no longer accepted signs the player out', () async {
    final container = _container(
      auth: fakeDio((request) => (401, {'status': 401, 'detail': 'Refresh token expired', 'code': 'INVALID_REFRESH_TOKEN'})),
      api: _bookings,
    );

    await expectLater(container.read(apiProvider).bookings('upcoming'), throwsA(isA<DioException>()));

    expect(container.read(sessionProvider), isNull);
    expect(container.read(sessionProvider.notifier).takeExpired(), isTrue);
  });

  test('being offline during a refresh keeps the player signed in', () async {
    final container = _container(
      auth: Dio()..httpClientAdapter = _Offline(),
      api: _bookings,
    );

    await expectLater(container.read(apiProvider).bookings('upcoming'), throwsA(isA<DioException>()));

    expect(container.read(sessionProvider)?.accessToken, 'old');
  });
}

class _Offline implements HttpClientAdapter {
  @override
  Future<ResponseBody> fetch(RequestOptions options, Stream<List<int>>? requestStream, Future<void>? cancelFuture) =>
      throw DioException.connectionError(requestOptions: options, reason: 'offline');

  @override
  void close({bool force = false}) {}
}
