import '../core/api_types.dart';

/// Where tapping a notification goes, from the ids it carries; null when there's nothing in the app to open.
String? notificationLink(AppNotification notification) {
  final data = notification.data;
  if (notification.type.startsWith('DEPOSIT_')) {
    return '/me/deposit';
  }
  if (data['conversationId'] case final id?) {
    return '/messages/$id';
  }
  if (data['bookingId'] case final id?) {
    return '/bookings/$id';
  }
  if (data['reviewId'] != null) {
    return '/me/reviews';
  }
  if (data['recurringBookingId'] != null) {
    return '/bookings';
  }
  if (data['venueSlug'] case final slug?) {
    return '/venues/$slug';
  }
  return null;
}
