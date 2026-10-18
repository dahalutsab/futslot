import 'package:flutter_test/flutter_test.dart';
import 'package:futslot/core/api_types.dart';
import 'package:futslot/shared/notification_link.dart';

// The web app's notification-link.spec.ts cases, with the app's own paths.

AppNotification _notification(String type, Map<String, String> data) =>
    AppNotification(id: 'n', type: type, title: 't', body: 'b', data: data, read: false, createdAt: '2026-10-17T00:00:00Z');

void main() {
  test('opens what the notification is about', () {
    expect(notificationLink(_notification('BOOKING_CONFIRMED', {'bookingId': 'b1', 'venueId': 'v'})), '/bookings/b1');
    expect(notificationLink(_notification('CHAT_MESSAGE', {'conversationId': 'c1', 'venueId': 'v'})), '/messages/c1');
    expect(notificationLink(_notification('WAITLIST_SLOT_FREE', {'venueId': 'v', 'venueSlug': 'dhuku'})), '/venues/dhuku');
  });

  test('sends deposit news to the deposit, whatever it carries', () {
    expect(notificationLink(_notification('DEPOSIT_FEE_TAKEN', {})), '/me/deposit');
  });

  test('has nowhere to go without ids', () {
    expect(notificationLink(_notification('VENUE_APPROVED', {})), isNull);
  });
}
