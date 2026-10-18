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
import '../../shared/format.dart';
import '../../shared/notification_bell.dart';
import '../../shared/widgets.dart';

/// How often an open list or conversation checks for new messages. There's no live connection.
const messagesPoll = Duration(seconds: 15);

/// The player's conversations with venues, one per venue, newest first. A conversation starts from a venue's
/// page or a booking.
class MessagesScreen extends ConsumerStatefulWidget {
  const MessagesScreen({super.key});

  @override
  ConsumerState<MessagesScreen> createState() => _MessagesScreenState();
}

class _MessagesScreenState extends ConsumerState<MessagesScreen> with WidgetsBindingObserver {
  final _filter = TextEditingController();

  List<ConversationSummary>? _conversations;

  String? _error;

  late final Timer _timer;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _timer = Timer.periodic(messagesPoll, (_) => _poll());
    _load();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _timer.cancel();
    _filter.dispose();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _load();
    }
  }

  /// Only while this tab is the one on screen.
  void _poll() {
    if (WidgetsBinding.instance.lifecycleState == AppLifecycleState.resumed && (ModalRoute.of(context)?.isCurrent ?? false)) {
      _load();
    }
  }

  Future<void> _load() async {
    try {
      final list = await ref.read(apiProvider).conversations();
      if (mounted) {
        setState(() {
          _conversations = list;
          _error = null;
        });
      }
    } catch (error) {
      if (mounted && _conversations == null) {
        setState(() => _error = problemOf(error).message);
      }
    }
  }

  Future<void> _open(ConversationSummary conversation) async {
    await context.push('/messages/${conversation.id}');
    if (mounted) {
      _load();
      ref.read(badgesProvider.notifier).refresh();
    }
  }

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    final list = _conversations;
    final term = _filter.text.trim().toLowerCase();
    final shown = term.isEmpty ? list : list?.where((conversation) => conversation.venueName.toLowerCase().contains(term)).toList();
    return Scaffold(
      appBar: AppBar(title: const Text('Messages'), actions: const [NotificationBell()]),
      body: ScreenBody(
        maxWidth: 720,
        onRefresh: _load,
        children: [
          if ((list?.length ?? 0) > 6) ...[
            TextField(
              controller: _filter,
              onChanged: (_) => setState(() {}),
              decoration: InputDecoration(
                hintText: 'Find a venue',
                prefixIcon: Icon(LucideIcons.search, size: 18, color: c.subtle),
              ),
            ),
            const SizedBox(height: 12),
          ],
          if (_error case final error?)
            ErrorPanel(message: error, onRetry: _load)
          else if (shown == null)
            const Skeleton(height: 168)
          else if (list!.isEmpty)
            const EmptyPanel(
              title: 'No messages yet',
              body: 'Ask a venue about courts, a booking or anything else: tap Message on its page or on your booking.',
            )
          else if (shown.isEmpty)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 32),
              child: Text('Nobody matches "${_filter.text.trim()}".', textAlign: TextAlign.center, style: TextStyle(color: c.muted)),
            )
          else
            FsCard(
              padding: EdgeInsets.zero,
              child: Column(
                children: [
                  for (final (index, conversation) in shown.indexed) ...[
                    if (index > 0) const Divider(),
                    _ConversationRow(conversation, onTap: () => _open(conversation)),
                  ],
                ],
              ),
            ),
        ],
      ),
    );
  }
}

class _ConversationRow extends StatelessWidget {
  const _ConversationRow(this.conversation, {required this.onTap});

  final ConversationSummary conversation;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    final unread = conversation.unread > 0;
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        child: Row(
          children: [
            ExcludeSemantics(
              child: CircleAvatar(
                radius: 20,
                backgroundColor: c.surface2,
                child: Text(
                  conversation.venueName.isEmpty ? '' : conversation.venueName[0].toUpperCase(),
                  style: TextStyle(fontWeight: FontWeight.w600, color: c.muted),
                ),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          conversation.venueName,
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(fontWeight: unread ? FontWeight.w600 : FontWeight.w500),
                        ),
                      ),
                      if (conversation.lastMessageAt case final at?)
                        Text(ago(at), style: TextStyle(fontSize: 12, color: c.subtle)),
                    ],
                  ),
                  const SizedBox(height: 2),
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          conversation.lastMessage ?? '',
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(fontSize: 14, color: unread ? c.text : c.muted),
                        ),
                      ),
                      if (unread) ...[
                        const SizedBox(width: 8),
                        Semantics(
                          label: '${conversation.unread} unread',
                          excludeSemantics: true,
                          child: Container(
                            padding: const EdgeInsets.symmetric(horizontal: 8),
                            decoration: BoxDecoration(color: c.ink, borderRadius: BorderRadius.circular(999)),
                            child: Text(
                              '${conversation.unread}',
                              style: TextStyle(fontSize: 12, fontWeight: FontWeight.w500, color: c.inkFg, fontFeatures: tabular),
                            ),
                          ),
                        ),
                      ],
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
