import 'package:dio/dio.dart';

import '../l10n/l10n.dart';

/// The API's error code and a sentence to show, for any failed request.
class Problem {
  const Problem(this.code, this.message);

  final String? code;
  final String message;
}

Problem get _offline => Problem('OFFLINE', l.offline);

Problem get _unknown => Problem(null, l.somethingWentWrong);

Problem problemOf(Object error) {
  if (error is DioException) {
    final body = error.response?.data;
    if (body is Map && body['detail'] is String) {
      return Problem(body['code'] as String?, body['detail'] as String);
    }
    if (error.response == null) {
      return _offline;
    }
  }
  return _unknown;
}
