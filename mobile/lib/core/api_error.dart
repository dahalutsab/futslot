import 'package:dio/dio.dart';

/// The API's error code and a sentence to show, for any failed request.
class Problem {
  const Problem(this.code, this.message);

  final String? code;
  final String message;
}

const _offline = Problem('OFFLINE', "Can't reach FutSlot. Check your connection and try again.");

const _unknown = Problem(null, 'Something went wrong. Please try again.');

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
