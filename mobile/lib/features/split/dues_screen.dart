import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/api.dart';
import '../../core/api_error.dart';
import '../../core/api_types.dart';
import '../../core/theme.dart';
import '../../shared/format.dart';
import '../../shared/widgets.dart';

/// What the player owes friends who booked games they played in.
class DuesScreen extends ConsumerStatefulWidget {
  const DuesScreen({super.key});

  @override
  ConsumerState<DuesScreen> createState() => _DuesScreenState();
}

class _DuesScreenState extends ConsumerState<DuesScreen> {
  List<OwedShare>? _dues;

  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final list = await ref.read(apiProvider).splitDues();
      if (mounted) {
        setState(() {
          _dues = list;
          _error = null;
        });
      }
    } catch (error) {
      if (mounted) {
        setState(() => _error = problemOf(error).message);
      }
    }
  }

  Future<void> _open(OwedShare due) async {
    await context.push('/bookings/${due.bookingId}/split');
    // They may have been ticked off meanwhile
    _load();
  }

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    final dues = _dues;
    return Scaffold(
      appBar: AppBar(title: const Text('What you owe')),
      body: ScreenBody(
        onRefresh: _load,
        children: [
          Text(
            'Your share of games other players booked. Pay them, and they tick you off.',
            style: TextStyle(color: c.muted),
          ),
          const SizedBox(height: 16),
          if (_error case final error?)
            ErrorPanel(message: error, onRetry: _load)
          else if (dues == null)
            const Skeleton(height: 128)
          else if (dues.isEmpty)
            const EmptyPanel(
              title: "You're all square",
              body: 'When someone adds you to a game they booked and splits the cost, your share shows up here.',
            )
          else ...[
            Text.rich(
              TextSpan(
                text: rupees(dues.fold<double>(0, (sum, due) => sum + due.amount)),
                style: const TextStyle(fontSize: 24, fontWeight: FontWeight.w600, fontFeatures: tabular),
                children: [
                  TextSpan(text: ' in all', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w400, color: c.muted)),
                ],
              ),
            ),
            const SizedBox(height: 12),
            for (final due in dues)
              Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: _DueCard(due, onTap: () => _open(due)),
              ),
          ],
        ],
      ),
    );
  }
}

class _DueCard extends StatelessWidget {
  const _DueCard(this.due, {required this.onTap});

  final OwedShare due;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    return FsCard(
      padding: EdgeInsets.zero,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(due.payTo.name, style: const TextStyle(fontWeight: FontWeight.w500)),
                    Text(
                      '${due.venueName} · ${dayLabel(due.startsAt.substring(0, 10))} · ${localClock(due.startsAt)}',
                      style: TextStyle(fontSize: 14, color: c.muted, fontFeatures: tabular),
                    ),
                    if (due.payTo.phone case final number?) ...[
                      const SizedBox(height: 4),
                      Text('Pay to ${phone(number)}', style: const TextStyle(fontSize: 14, fontFeatures: tabular)),
                    ],
                  ],
                ),
              ),
              const SizedBox(width: 12),
              Text(rupees(due.amount), style: const TextStyle(fontWeight: FontWeight.w600, fontFeatures: tabular)),
            ],
          ),
        ),
      ),
    );
  }
}
