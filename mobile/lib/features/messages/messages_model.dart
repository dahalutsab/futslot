import '../../core/api_types.dart';
import '../../shared/format.dart';

class DayGroup {
  const DayGroup({required this.day, required this.label, required this.messages});

  final String day;
  final String label;
  final List<ChatMessage> messages;
}

/// Adds [incoming] (any order, possibly overlapping) to [known], oldest first, without duplicates.
List<ChatMessage> merge(List<ChatMessage> known, List<ChatMessage> incoming) {
  final byId = {for (final message in known) message.id: message, for (final message in incoming) message.id: message};
  return byId.values.toList()
    ..sort((a, b) {
      final byTime = DateTime.parse(a.createdAt).compareTo(DateTime.parse(b.createdAt));
      return byTime != 0 ? byTime : a.id.compareTo(b.id);
    });
}

/// Oldest-first messages split by the venue's calendar day, labelled "Today", "Yesterday" or "Thu 9 Oct".
List<DayGroup> byDay(List<ChatMessage> messages, String timeZone, DateTime now) {
  final todayKey = today(timeZone, now);
  final groups = <DayGroup>[];
  for (final message in messages) {
    final day = dayOf(message.createdAt, timeZone);
    if (groups.isEmpty || groups.last.day != day) {
      groups.add(DayGroup(day: day, label: _dayName(day, todayKey), messages: []));
    }
    groups.last.messages.add(message);
  }
  return groups;
}

String _dayName(String day, String todayKey) {
  if (day == todayKey) {
    return 'Today';
  }
  if (day == addDays(todayKey, -1)) {
    return 'Yesterday';
  }
  return dayLabel(day);
}

/// Whether two neighbouring messages read as one burst: same side and sender, within five minutes.
bool continues(ChatMessage? previous, ChatMessage message) =>
    previous != null &&
    previous.fromYou == message.fromYou &&
    previous.senderName == message.senderName &&
    DateTime.parse(message.createdAt).difference(DateTime.parse(previous.createdAt)) < const Duration(minutes: 5);
