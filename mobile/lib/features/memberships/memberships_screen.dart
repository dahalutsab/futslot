import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/api.dart';
import '../../core/api_error.dart';
import '../../core/api_types.dart';
import '../../core/theme.dart';
import '../../shared/format.dart';
import '../../shared/labels.dart';
import '../../shared/widgets.dart';

/// The player's game packs and passes, at every venue.
class MembershipsScreen extends ConsumerStatefulWidget {
  const MembershipsScreen({super.key});

  @override
  ConsumerState<MembershipsScreen> createState() => _MembershipsScreenState();
}

class _MembershipsScreenState extends ConsumerState<MembershipsScreen> {
  List<Membership>? _memberships;

  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final list = await ref.read(apiProvider).memberships();
      if (mounted) {
        setState(() {
          _memberships = list;
          _error = null;
        });
      }
    } catch (error) {
      if (mounted) {
        setState(() => _error = problemOf(error).message);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    final list = _memberships;
    return Scaffold(
      appBar: AppBar(title: const Text('Memberships')),
      body: ScreenBody(
        onRefresh: _load,
        children: [
          Text(
            "Game packs and passes you bought at venues. They're used when you book there.",
            style: TextStyle(color: c.muted),
          ),
          const SizedBox(height: 16),
          if (_error case final error?)
            ErrorPanel(message: error, onRetry: _load)
          else if (list == null)
            const Skeleton(height: 128)
          else if (list.isEmpty)
            const EmptyPanel(
              title: 'No memberships yet',
              body: 'Venues that sell game packs or passes list them on their page.',
            )
          else
            for (final m in list) Padding(padding: const EdgeInsets.only(bottom: 12), child: _MembershipCard(m)),
        ],
      ),
    );
  }
}

class _MembershipCard extends StatelessWidget {
  const _MembershipCard(this.m);

  final Membership m;

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    final (label, colors) = switch (m.status) {
      MembershipStatus.active => ('Active', c.confirmed),
      MembershipStatus.pendingPayment => ('Payment being checked', c.verify),
      MembershipStatus.expired => ('Expired', c.blocked),
      MembershipStatus.cancelled => ('Cancelled', c.blocked),
    };
    final terms = planTerms(m.planType, gamesIncluded: m.gamesIncluded, discountPercent: m.discountPercent);
    return FsCard(
      color: m.active ? null : c.surface2,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(m.planName, style: const TextStyle(fontWeight: FontWeight.w600)),
                    Text(m.venue.name, style: TextStyle(fontSize: 14, color: c.muted)),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                decoration: BoxDecoration(color: colors.bg, borderRadius: BorderRadius.circular(999)),
                child: Text(label, style: TextStyle(fontSize: 12, fontWeight: FontWeight.w500, color: colors.fg)),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Text(
            m.gamesRemaining != null ? '$terms · ${m.gamesRemaining} left' : terms,
            style: const TextStyle(fontSize: 14, fontFeatures: tabular),
          ),
          Text(
            '${m.status == MembershipStatus.expired ? 'Ended' : 'Until'} ${dayLabel(m.expiresOn)}',
            style: TextStyle(fontSize: 14, color: c.muted),
          ),
          if (m.active && m.venue.slug != null) ...[
            const SizedBox(height: 12),
            FilledButton(onPressed: () => context.push('/venues/${m.venue.slug}'), child: const Text('Book with it')),
          ],
        ],
      ),
    );
  }
}
