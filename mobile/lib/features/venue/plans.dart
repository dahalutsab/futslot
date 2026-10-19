import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/api.dart';
import '../../core/api_error.dart';
import '../../core/api_types.dart';
import '../../core/session.dart';
import '../../core/theme.dart';
import '../../l10n/l10n.dart';
import '../../shared/format.dart';
import '../../shared/labels.dart';
import '../../shared/pay_form.dart';
import '../../shared/widgets.dart';

/// A venue's game packs and passes, bought by paying the venue and reporting it.
class VenuePlans extends ConsumerStatefulWidget {
  const VenuePlans({super.key, required this.venue});

  final VenueDetail venue;

  @override
  ConsumerState<VenuePlans> createState() => _VenuePlansState();
}

class _VenuePlansState extends ConsumerState<VenuePlans> {
  var _plans = <MembershipPlan>[];

  /// The plan being bought.
  String? _buying;

  var _busy = false;

  String? _error;

  Api get _api => ref.read(apiProvider);

  @override
  void initState() {
    super.initState();
    _api.membershipPlans(widget.venue.slug).then((plans) {
      if (mounted) {
        setState(() => _plans = plans.where((plan) => plan.active).toList());
      }
    }, onError: (_) {});
  }

  void _start(MembershipPlan plan) {
    if (ref.read(sessionProvider) == null) {
      context.push(Uri(path: '/login', queryParameters: {'next': '/venues/${widget.venue.slug}'}).toString());
      return;
    }
    setState(() {
      _error = null;
      _buying = plan.id;
    });
  }

  Future<void> _buy(MembershipPlan plan, PaymentClaim? claim) async {
    if (_busy) {
      return;
    }
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await _api.buyMembership(plan.id, claim);
      if (mounted) {
        setState(() => _buying = null);
        context.push('/me/memberships');
      }
    } catch (error) {
      if (mounted) {
        setState(() => _error = problemOf(error).message);
      }
    } finally {
      if (mounted) {
        setState(() => _busy = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_plans.isEmpty) {
      return const SizedBox.shrink();
    }
    final c = context.fs;
    final text = Theme.of(context).textTheme;
    return Padding(
      padding: const EdgeInsets.only(top: 32),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Semantics(header: true, child: Text(l.memberships, style: text.titleLarge)),
          Text(l.membershipsPitch, style: TextStyle(color: c.muted)),
          const SizedBox(height: 12),
          for (final plan in _plans)
            Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: FsCard(
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
                              Text(plan.name, style: const TextStyle(fontWeight: FontWeight.w600)),
                              Text(
                                planTerms(
                                  plan.type,
                                  gamesIncluded: plan.gamesIncluded,
                                  discountPercent: plan.discountPercent,
                                  validityDays: plan.validityDays,
                                ),
                                style: TextStyle(fontSize: 14, color: c.muted),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(width: 12),
                        Text(
                          plan.price > 0 ? rupees(plan.price) : l.free,
                          style: const TextStyle(fontWeight: FontWeight.w600, fontFeatures: tabular),
                        ),
                      ],
                    ),
                    if (plan.description case final description?) ...[
                      const SizedBox(height: 8),
                      Text(description, style: TextStyle(fontSize: 14, color: c.muted)),
                    ],
                    const SizedBox(height: 12),
                    if (_buying == plan.id)
                      ..._buyForm(context, plan)
                    else
                      OutlinedButton(onPressed: () => _start(plan), child: Text(l.buy)),
                  ],
                ),
              ),
            ),
        ],
      ),
    );
  }

  List<Widget> _buyForm(BuildContext context, MembershipPlan plan) {
    final c = context.fs;
    return [
      if (plan.price > 0) ...[
        PayForm(
          amount: plan.price,
          instructions: widget.venue.paymentInstructions,
          busy: _busy,
          error: _error,
          label: l.iPaid,
          onPaid: (claim) => _buy(plan, claim),
        ),
        const SizedBox(height: 8),
        Text(
          l.membershipChecks(widget.venue.name),
          style: TextStyle(fontSize: 14, color: c.subtle),
        ),
      ] else ...[
        if (_error case final error?) ...[Notice(error), const SizedBox(height: 8)],
        Align(
          alignment: Alignment.centerLeft,
          child: FilledButton(
            onPressed: _busy ? null : () => _buy(plan, null),
            child: BusyLabel(busy: _busy, label: l.getIt, busyLabel: l.gettingIt),
          ),
        ),
      ],
      TextButton(
        onPressed: () => setState(() => _buying = null),
        style: TextButton.styleFrom(foregroundColor: c.muted, padding: EdgeInsets.zero),
        child: Text(l.notNow),
      ),
    ];
  }
}
