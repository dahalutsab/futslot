import 'package:flutter_test/flutter_test.dart';
import 'package:futslot/core/api_types.dart';
import 'package:futslot/features/messages/messages_model.dart';
import 'package:timezone/data/latest_10y.dart' as tzdata;

// The same cases as the player web app's messages-model.spec.ts.

const _zone = 'Asia/Kathmandu';

ChatMessage _message(String id, String createdAt, {bool fromYou = false, String senderName = 'Sita Rai'}) => ChatMessage(
      id: id,
      senderName: senderName,
      fromYou: fromYou,
      bookingId: null,
      body: id,
      readAt: null,
      createdAt: createdAt,
    );

void main() {
  setUpAll(tzdata.initializeTimeZones);

  test('merges pages oldest first without duplicates', () {
    final known = [_message('b', '2026-10-07T10:00:00Z'), _message('c', '2026-10-07T11:00:00Z')];
    final merged = merge(known, [_message('c', '2026-10-07T11:00:00Z'), _message('a', '2026-10-07T09:00:00Z')]);
    expect(merged.map((m) => m.id), ['a', 'b', 'c']);
  });

  test('groups by the venue day, not the UTC day', () {
    // 20:00 UTC on the 6th is 01:45 on the 7th in Kathmandu
    final groups = byDay([
      _message('a', '2026-10-05T12:00:00Z'),
      _message('b', '2026-10-06T20:00:00Z'),
      _message('c', '2026-10-07T03:00:00Z'),
    ], _zone, DateTime.parse('2026-10-07T06:00:00Z'));
    expect(groups.map((g) => (g.label, g.messages.length)), [('Mon 5 Oct', 1), ('Today', 2)]);
  });

  test('labels yesterday', () {
    final groups = byDay([_message('a', '2026-10-06T06:00:00Z')], _zone, DateTime.parse('2026-10-07T06:00:00Z'));
    expect(groups.first.label, 'Yesterday');
  });

  test('runs a burst together only for the same sender within five minutes', () {
    final first = _message('a', '2026-10-07T10:00:00Z', fromYou: true, senderName: 'Utsab');
    expect(continues(first, _message('b', '2026-10-07T10:04:00Z', fromYou: true, senderName: 'Utsab')), isTrue);
    expect(continues(first, _message('b', '2026-10-07T10:06:00Z', fromYou: true, senderName: 'Utsab')), isFalse);
    expect(continues(first, _message('b', '2026-10-07T10:01:00Z', fromYou: true, senderName: 'Hari')), isFalse);
    expect(continues(null, first), isFalse);
  });
}
