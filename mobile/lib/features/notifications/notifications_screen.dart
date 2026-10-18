import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/api.dart';
import '../../core/api_error.dart';
import '../../core/api_types.dart';
import '../../core/badges.dart';
import '../../core/theme.dart';
import '../../shared/format.dart';
import '../../shared/notification_link.dart';
import '../../shared/widgets.dart';

/// Everything FutSlot told the player: newest first, unread marked, each opening what it's about.
class NotificationsScreen extends ConsumerStatefulWidget {
  const NotificationsScreen({super.key});

  @override
  ConsumerState<NotificationsScreen> createState() => _NotificationsScreenState();
}

class _NotificationsScreenState extends ConsumerState<NotificationsScreen> {
  List<AppNotification>? _items;

  var _page = 0;

  var _hasMore = false;

  String? _error;

  Api get _api => ref.read(apiProvider);

  @override
  void initState() {
    super.initState();
    _load(0);
  }

  Future<void> _load(int page) async {
    try {
      final result = await _api.notifications(page: page);
      if (mounted) {
        setState(() {
          _items = page == 0 ? result.content : [...?_items, ...result.content];
          _page = page;
          _hasMore = page + 1 < result.totalPages;
          _error = null;
        });
      }
    } catch (error) {
      if (mounted) {
        setState(() => _error = problemOf(error).message);
      }
    }
  }

  void _open(AppNotification notification) {
    if (!notification.read) {
      setState(() => _items = [for (final n in _items!) n.id == notification.id ? n.markedRead() : n]);
      _api.readNotification(notification.id).then((_) => ref.read(badgesProvider.notifier).refresh(), onError: (_) {});
    }
    if (notificationLink(notification) case final link?) {
      context.go(link);
    }
  }

  Future<void> _readAll() async {
    try {
      await _api.readAllNotifications();
      if (mounted) {
        setState(() => _items = [for (final n in _items!) n.markedRead()]);
      }
      ref.read(badgesProvider.notifier).refresh();
    } catch (error) {
      if (mounted) {
        setState(() => _error = problemOf(error).message);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    final items = _items;
    return Scaffold(
      appBar: AppBar(
        title: const Text('Notifications'),
        actions: [
          if (items?.any((n) => !n.read) ?? false)
            TextButton(
              onPressed: _readAll,
              style: TextButton.styleFrom(foregroundColor: c.muted),
              child: const Text('Mark all read', style: TextStyle(fontSize: 14)),
            ),
        ],
      ),
      body: ScreenBody(
        onRefresh: () => _load(0),
        children: [
          if (_error case final error?)
            ErrorPanel(message: error, onRetry: () => _load(0))
          else if (items == null)
            const Skeleton(height: 192)
          else if (items.isEmpty)
            const EmptyPanel(
              title: 'Nothing yet',
              body: 'Booking confirmations, reminders and replies from venues show up here.',
            )
          else ...[
            FsCard(
              padding: EdgeInsets.zero,
              child: Column(
                children: [
                  for (final (index, n) in items.indexed) ...[
                    if (index > 0) const Divider(),
                    _NotificationRow(n, onTap: () => _open(n)),
                  ],
                ],
              ),
            ),
            if (_hasMore) ...[
              const SizedBox(height: 16),
              Center(child: OutlinedButton(onPressed: () => _load(_page + 1), child: const Text('Show older'))),
            ],
          ],
        ],
      ),
    );
  }
}

class _NotificationRow extends StatelessWidget {
  const _NotificationRow(this.notification, {required this.onTap});

  final AppNotification notification;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    final n = notification;
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Padding(
              padding: const EdgeInsets.only(top: 8),
              child: Semantics(
                label: n.read ? null : 'Unread',
                child: Container(
                  width: 8,
                  height: 8,
                  decoration: BoxDecoration(shape: BoxShape.circle, color: n.read ? Colors.transparent : c.ring),
                ),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Expanded(
                        child: Text(n.title, style: TextStyle(fontWeight: n.read ? FontWeight.w500 : FontWeight.w600)),
                      ),
                      const SizedBox(width: 8),
                      Padding(
                        padding: const EdgeInsets.only(top: 3),
                        child: Text(ago(n.createdAt), style: TextStyle(fontSize: 12, color: c.subtle)),
                      ),
                    ],
                  ),
                  const SizedBox(height: 2),
                  Text(n.body, style: TextStyle(fontSize: 14, color: c.muted)),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

