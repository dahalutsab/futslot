import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/api.dart';
import '../../core/api_error.dart';
import '../../core/api_types.dart';
import '../../core/badges.dart';
import '../../core/theme.dart';
import '../../l10n/l10n.dart';
import '../../shared/booking_model.dart';
import '../../shared/format.dart';
import '../../shared/status_chip.dart';
import '../../shared/widgets.dart';
import 'messages_model.dart';
import 'messages_screen.dart';

const _zone = 'Asia/Kathmandu';

/// One conversation with a venue. Opened by its id, or from a venue's page or a booking with the venue (and
/// the booking it's about): then it carries on the conversation with that venue if there is one, and the first
/// message is sent to the venue, tied to the booking.
class ThreadScreen extends ConsumerStatefulWidget {
  const ThreadScreen({super.key, this.conversationId, this.venueId, this.venueName, this.bookingId});

  final String? conversationId;
  final String? venueId;
  final String? venueName;
  final String? bookingId;

  @override
  ConsumerState<ThreadScreen> createState() => _ThreadScreenState();
}

class _ThreadScreenState extends ConsumerState<ThreadScreen> with WidgetsBindingObserver {
  final _draft = TextEditingController();

  late String? _id = widget.conversationId;

  /// Still writing to the venue from its page or a booking; the first message goes through it.
  late var _composing = widget.venueId != null;

  ConversationSummary? _conversation;

  var _messages = <ChatMessage>[];

  var _loading = true;

  String? _error;

  var _hasEarlier = false;

  var _loadingEarlier = false;

  var _sending = false;

  String? _sendError;

  /// Bookings that messages are about, fetched once each; null while loading or when it couldn't be.
  final _bookings = <String, Booking?>{};

  late final Timer _timer;

  Api get _api => ref.read(apiProvider);

  String get _title => _conversation?.venueName ?? widget.venueName ?? '';

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _timer = Timer.periodic(messagesPoll, (_) => _poll());
    _open();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _timer.cancel();
    _draft.dispose();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _refresh();
    }
  }

  void _poll() {
    if (WidgetsBinding.instance.lifecycleState == AppLifecycleState.resumed) {
      _refresh();
    }
  }

  Future<void> _open() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final conversations = await _api.conversations();
      _conversation = conversations
          .where((c) => _id != null ? c.id == _id : c.venueId == widget.venueId)
          .firstOrNull;
      _id = _conversation?.id;
      if (_id case final id?) {
        final page = await _api.messages(id);
        _messages = merge(const [], page);
        _hasEarlier = page.length == Api.messagesPage;
        _loadBookings(page);
        await _markRead(id, page);
      }
    } catch (error) {
      _error = problemOf(error).message;
    }
    if (mounted) {
      setState(() => _loading = false);
    }
  }

  /// Picks up new messages.
  Future<void> _refresh() async {
    final id = _id;
    if (id == null || _loading || _error != null) {
      return;
    }
    try {
      final page = await _api.messages(id);
      if (!mounted) {
        return;
      }
      setState(() => _messages = merge(_messages, page));
      _loadBookings(page);
      await _markRead(id, page);
    } catch (_) {
      // The next check tries again
    }
  }

  Future<void> _earlier() async {
    final id = _id;
    if (id == null || _messages.isEmpty || _loadingEarlier) {
      return;
    }
    setState(() => _loadingEarlier = true);
    try {
      final page = await _api.messages(id, before: _messages.first.createdAt);
      if (mounted) {
        setState(() {
          _messages = merge(_messages, page);
          _hasEarlier = page.length == Api.messagesPage;
        });
        _loadBookings(page);
      }
    } catch (error) {
      if (mounted) {
        setState(() => _sendError = problemOf(error).message);
      }
    } finally {
      if (mounted) {
        setState(() => _loadingEarlier = false);
      }
    }
  }

  Future<void> _markRead(String id, List<ChatMessage> page) async {
    if (!page.any((m) => !m.fromYou && m.readAt == null) && (_conversation?.unread ?? 0) == 0) {
      return;
    }
    await _api.readConversation(id);
    _conversation = _conversation?.copyWith(unread: 0);
    ref.read(badgesProvider.notifier).refresh();
  }

  void _loadBookings(List<ChatMessage> page) {
    for (final bookingId in {for (final m in page) ?m.bookingId}) {
      if (_bookings.containsKey(bookingId)) {
        continue;
      }
      _bookings[bookingId] = null;
      _api.booking(bookingId).then((booking) {
        if (mounted) {
          setState(() => _bookings[bookingId] = booking);
        }
      }, onError: (_) {});
    }
  }

  Future<void> _send() async {
    final body = _draft.text.trim();
    if (_sending) {
      return;
    }
    if (body.isEmpty) {
      setState(() => _sendError = l.typeMessageFirst);
      return;
    }
    setState(() {
      _sending = true;
      _sendError = null;
    });
    try {
      final ChatMessage sent;
      if (_composing) {
        // From a venue or booking the first message goes to the venue, tied to the booking; then it's a reply
        sent = await _api.messageVenue(widget.venueId!, body, bookingId: widget.bookingId);
        final conversations = await _api.conversations();
        _conversation = conversations.where((c) => c.venueId == widget.venueId).firstOrNull;
        _id = _conversation?.id;
        _composing = false;
      } else {
        sent = await _api.reply(_id!, body);
      }
      _draft.clear();
      if (mounted) {
        setState(() => _messages = merge(_messages, [sent]));
        _loadBookings([sent]);
      }
    } catch (error) {
      if (mounted) {
        setState(() => _sendError = problemOf(error).message);
      }
    } finally {
      if (mounted) {
        setState(() => _sending = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    final slug = _conversation?.venueSlug;
    return Scaffold(
      appBar: AppBar(
        title: Text(_title, overflow: TextOverflow.ellipsis),
        actions: [
          if (slug != null)
            TextButton(
              onPressed: () => context.push('/venues/$slug'),
              style: TextButton.styleFrom(foregroundColor: c.muted),
              child: Text(l.venue, style: const TextStyle(fontSize: 14)),
            ),
        ],
        shape: Border(bottom: BorderSide(color: c.border)),
      ),
      body: Column(
        children: [
          Expanded(child: _thread(context)),
          _composer(context),
        ],
      ),
    );
  }

  Widget _thread(BuildContext context) {
    final c = context.fs;
    if (_error case final error?) {
      return ScreenBody(children: [ErrorPanel(message: error, onRetry: _open)]);
    }
    if (_loading) {
      return const ScreenBody(children: [Skeleton(height: 48), SizedBox(height: 12), Skeleton(height: 48)]);
    }
    if (_messages.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(32),
          child: Text(
            (widget.bookingId != null ? l.askVenueBooking : l.askVenue)(_title),
            textAlign: TextAlign.center,
            style: TextStyle(color: c.muted),
          ),
        ),
      );
    }
    final lastYours = _messages.lastWhere((m) => m.fromYou, orElse: () => _messages.first);
    final seenId = lastYours.fromYou && lastYours.readAt != null ? lastYours.id : null;
    final rows = <Widget>[
      if (_hasEarlier)
        Center(
          child: Padding(
            padding: const EdgeInsets.only(bottom: 16),
            child: OutlinedButton(
              onPressed: _loadingEarlier ? null : _earlier,
              child: Text(_loadingEarlier ? l.loadingMore : l.showEarlier, style: const TextStyle(fontSize: 14)),
            ),
          ),
        ),
      for (final group in byDay(_messages, _zone, DateTime.now())) ...[
        _DaySeparator(group.label),
        for (final (i, message) in group.messages.indexed)
          _Bubble(
            message: message,
            joined: continues(i > 0 ? group.messages[i - 1] : null, message),
            last: i == group.messages.length - 1 || !continues(message, group.messages[i + 1]),
            seen: message.id == seenId,
            booking: message.bookingId == null ? null : _bookings[message.bookingId],
          ),
      ],
    ];
    // Reversed, so it opens on the newest message and stays there as messages arrive
    return ListView.builder(
      reverse: true,
      padding: const EdgeInsets.fromLTRB(12, 16, 12, 8),
      itemCount: rows.length,
      itemBuilder: (context, index) => rows[rows.length - 1 - index],
    );
  }

  Widget _composer(BuildContext context) {
    final c = context.fs;
    return Container(
      decoration: BoxDecoration(color: c.surface, border: Border(top: BorderSide(color: c.border))),
      padding: const EdgeInsets.fromLTRB(12, 8, 12, 8),
      child: SafeArea(
        top: false,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            if (_sendError case final error?) ...[Notice(error), const SizedBox(height: 8)],
            Row(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Expanded(
                  child: TextField(
                    controller: _draft,
                    enabled: _error == null && !_loading,
                    minLines: 1,
                    maxLines: 5,
                    maxLength: 2000,
                    textCapitalization: TextCapitalization.sentences,
                    onChanged: (_) {
                      if (_sendError != null) {
                        setState(() => _sendError = null);
                      }
                    },
                    decoration: InputDecoration(
                      hintText: l.messageTo(_title),
                      counterText: '',
                      border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide(color: c.border)),
                      enabledBorder:
                          OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide(color: c.border)),
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                SizedBox.square(
                  dimension: 46,
                  child: FilledButton(
                    onPressed: _sending || _loading || _error != null ? null : _send,
                    style: FilledButton.styleFrom(
                      padding: EdgeInsets.zero,
                      minimumSize: const Size.square(46),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                    ),
                    child: Icon(LucideIcons.sendHorizontal, size: 20, semanticLabel: l.send),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _DaySeparator extends StatelessWidget {
  const _DaySeparator(this.label);

  final String label;

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 12),
      child: Row(
        children: [
          Expanded(child: Divider(color: c.border)),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 12),
            child: Text(label, style: TextStyle(fontSize: 12, color: c.subtle)),
          ),
          Expanded(child: Divider(color: c.border)),
        ],
      ),
    );
  }
}

class _Bubble extends StatelessWidget {
  const _Bubble({required this.message, required this.joined, required this.last, required this.seen, this.booking});

  final ChatMessage message;

  /// Carries on the message before it (same sender, within minutes).
  final bool joined;

  /// The last of a run, which shows the time.
  final bool last;

  /// The venue has read it.
  final bool seen;
  final Booking? booking;

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    final mine = message.fromYou;
    const round = Radius.circular(18);
    const tight = Radius.circular(6);
    final shape = BorderRadius.only(
      topLeft: !mine && joined ? tight : round,
      topRight: mine && joined ? tight : round,
      bottomLeft: !mine && !last ? tight : round,
      bottomRight: mine && !last ? tight : round,
    );
    final b = booking;
    return Padding(
      padding: EdgeInsets.only(top: joined ? 2 : 12),
      child: Align(
        alignment: mine ? Alignment.centerRight : Alignment.centerLeft,
        child: ConstrainedBox(
          constraints: BoxConstraints(maxWidth: MediaQuery.sizeOf(context).width * 0.8),
          child: Column(
            crossAxisAlignment: mine ? CrossAxisAlignment.end : CrossAxisAlignment.start,
            children: [
              if (b != null)
                Padding(
                  padding: const EdgeInsets.only(bottom: 4),
                  child: Material(
                    color: c.surface,
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8), side: BorderSide(color: c.border)),
                    child: InkWell(
                      borderRadius: BorderRadius.circular(8),
                      onTap: () => context.push('/bookings/${b.id}'),
                      child: Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                        child: Wrap(
                          spacing: 8,
                          runSpacing: 4,
                          crossAxisAlignment: WrapCrossAlignment.center,
                          children: [
                            Icon(LucideIcons.calendarDays, size: 16, color: c.muted),
                            Text(
                              '${dayLabel(b.startsAt.substring(0, 10))} · ${localClock(b.startsAt)} · ${b.court.name}',
                              style: const TextStyle(fontSize: 14, fontFeatures: tabular),
                            ),
                            StatusChip(bookingChip(b)),
                          ],
                        ),
                      ),
                    ),
                  ),
                ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                decoration: BoxDecoration(
                  color: mine ? c.ink : c.surface,
                  border: mine ? null : Border.all(color: c.border),
                  borderRadius: shape,
                ),
                child: SelectableText(message.body, style: TextStyle(color: mine ? c.inkFg : c.text)),
              ),
              if (last)
                Padding(
                  padding: const EdgeInsets.fromLTRB(4, 4, 4, 0),
                  child: Text(
                    '${clockAt(message.createdAt, _zone)}${seen ? l.seen : ''}',
                    style: TextStyle(fontSize: 12, color: c.subtle, fontFeatures: tabular),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}
