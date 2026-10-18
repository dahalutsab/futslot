import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/api.dart';
import '../../core/api_error.dart';
import '../../core/api_types.dart';
import '../../core/theme.dart';
import '../../shared/format.dart';
import '../../shared/pay_form.dart';
import '../../shared/widgets.dart';

/// Where FutSlot can send a deposit back.
const _sendMethods = [
  (PaymentMethod.esewa, 'eSewa'),
  (PaymentMethod.khalti, 'Khalti'),
  (PaymentMethod.bankTransfer, 'Bank account'),
];

/// The player's FutSlot booking deposit: how much is in it, topping it up, taking it back, and every change.
/// Some venues only take bookings from players whose deposit is full.
class DepositScreen extends ConsumerStatefulWidget {
  const DepositScreen({super.key, this.back});

  /// Where the player was booking, to go back once the deposit is full.
  final String? back;

  @override
  ConsumerState<DepositScreen> createState() => _DepositScreenState();
}

class _DepositScreenState extends ConsumerState<DepositScreen> {
  final _sendTo = TextEditingController();

  Deposit? _deposit;

  String? _error;

  String? _topUpError;

  var _toppingUp = false;

  var _withdrawOpen = false;

  var _sendMethod = PaymentMethod.esewa;

  var _withdrawing = false;

  String? _withdrawError;

  Api get _api => ref.read(apiProvider);

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _sendTo.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    try {
      final deposit = await _api.deposit();
      if (mounted) {
        setState(() {
          _deposit = deposit;
          _error = null;
        });
      }
    } catch (error) {
      if (mounted) {
        setState(() => _error = problemOf(error).message);
      }
    }
  }

  Future<void> _topUp(PaymentClaim claim) async {
    if (_toppingUp) {
      return;
    }
    setState(() {
      _toppingUp = true;
      _topUpError = null;
    });
    try {
      await _api.topUp(claim);
      await _load();
    } catch (error) {
      if (mounted) {
        setState(() => _topUpError = problemOf(error).message);
      }
    } finally {
      if (mounted) {
        setState(() => _toppingUp = false);
      }
    }
  }

  Future<void> _withdraw() async {
    if (_withdrawing) {
      return;
    }
    final sendTo = _sendTo.text.trim();
    if (sendTo.length < 5) {
      setState(() => _withdrawError = 'Enter the number or account to send it to.');
      return;
    }
    setState(() {
      _withdrawing = true;
      _withdrawError = null;
    });
    try {
      await _api.withdraw(_sendMethod, sendTo);
      _withdrawOpen = false;
      await _load();
    } catch (error) {
      if (mounted) {
        setState(() => _withdrawError = problemOf(error).message);
      }
    } finally {
      if (mounted) {
        setState(() => _withdrawing = false);
      }
    }
  }

  Future<void> _keep(DepositWithdrawal withdrawal) async {
    if (_withdrawing) {
      return;
    }
    setState(() => _withdrawing = true);
    try {
      await _api.keepDeposit(withdrawal.id);
      await _load();
    } catch (error) {
      if (mounted) {
        setState(() => _error = problemOf(error).message);
      }
    } finally {
      if (mounted) {
        setState(() => _withdrawing = false);
      }
    }
  }

  static String _entryLabel(DepositEntry entry) => switch (entry.type) {
        DepositEntryType.topUp => 'Topped up',
        DepositEntryType.fee => 'Fee to ${entry.venueName} · ${entry.bookingReference}',
        DepositEntryType.withdrawal => 'Taken back',
        DepositEntryType.withdrawalCancelled => 'Kept after all',
      };

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    final deposit = _deposit;
    return Scaffold(
      appBar: AppBar(title: const Text('Booking deposit')),
      body: ScreenBody(
        onRefresh: _load,
        children: [
          Text(
            'Some venues only take bookings from players who keep ${rupees(depositAmount)} with FutSlot. It isn\'t spent '
            "on your games: it only pays a venue's fee if you cancel late or don't show up. You can take it back "
            'whenever you have no games booked there.',
            style: TextStyle(color: c.muted),
          ),
          if (_error case final error?) ...[const SizedBox(height: 16), Notice(error)],
          const SizedBox(height: 20),
          if (deposit != null)
            ..._body(context, deposit)
          else if (_error == null)
            const Skeleton(height: 160),
        ],
      ),
    );
  }

  List<Widget> _body(BuildContext context, Deposit d) {
    final c = context.fs;
    final text = Theme.of(context).textTheme;
    final withdrawal = d.withdrawal;
    final full = d.toAdd == 0;
    return [
      FsCard(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('In your deposit', style: TextStyle(fontSize: 14, color: c.muted)),
            const SizedBox(height: 4),
            Text.rich(
              TextSpan(
                text: rupees(d.balance),
                style: const TextStyle(fontSize: 30, fontWeight: FontWeight.w600, fontFeatures: tabular),
                children: [
                  TextSpan(
                    text: ' of ${rupees(d.required)}',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.w400, color: c.muted),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 12),
            ClipRRect(
              borderRadius: BorderRadius.circular(999),
              child: LinearProgressIndicator(
                value: d.required == 0 ? 0 : (d.balance / d.required).clamp(0, 1),
                minHeight: 8,
                backgroundColor: c.surface2,
                color: c.confirmed.line,
                semanticsLabel: 'Deposit',
              ),
            ),
            if (full) ...[
              const SizedBox(height: 12),
              Row(
                children: [
                  Icon(LucideIcons.shieldCheck, size: 16, color: c.confirmed.fg),
                  const SizedBox(width: 6),
                  Text('Full: you can book any venue.', style: TextStyle(color: c.confirmed.fg)),
                ],
              ),
              if (widget.back != null) ...[
                const SizedBox(height: 12),
                FilledButton(
                  // The booking screen is underneath when the player came from it
                  onPressed: () => context.canPop() ? context.pop() : context.go(widget.back!),
                  child: const Text('Back to your booking'),
                ),
              ],
            ],
          ],
        ),
      ),
      const SizedBox(height: 16),
      if (withdrawal != null)
        FsCard(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('${rupees(withdrawal.amount)} is coming back to you', style: text.titleMedium),
              const SizedBox(height: 4),
              Text(
                'FutSlot sends it to your ${methodLabel(withdrawal.method)} ${withdrawal.sendTo} on '
                "${dayLabel(dayOf(withdrawal.dueAt, 'Asia/Kathmandu'))}. Until then you can't book venues that need "
                'the deposit.',
                style: TextStyle(color: c.muted),
              ),
              const SizedBox(height: 12),
              OutlinedButton(
                onPressed: _withdrawing ? null : () => _keep(withdrawal),
                child: const Text('Keep my deposit instead'),
              ),
            ],
          ),
        )
      else if (d.topUpBeingChecked)
        FsCard(
          padding: const EdgeInsets.all(20),
          color: c.verify.bg,
          borderColor: c.verify.line,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Top-up sent', style: text.titleMedium?.copyWith(color: c.verify.fg)),
              const SizedBox(height: 4),
              Text(
                "FutSlot is checking it, usually within a few hours. You can book once it's in.",
                style: TextStyle(color: c.verify.fg),
              ),
            ],
          ),
        )
      else if (d.toAdd > 0)
        FsCard(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text('Add ${rupees(d.toAdd)}', style: text.titleMedium),
              const SizedBox(height: 4),
              Text('Pay FutSlot directly, then tell us the transaction ID.', style: TextStyle(fontSize: 14, color: c.muted)),
              const SizedBox(height: 12),
              PayForm(
                amount: d.toAdd,
                instructions: d.paymentInstructions,
                busy: _toppingUp,
                error: _topUpError,
                onPaid: _topUp,
              ),
            ],
          ),
        ),
      if (d.balance > 0 && withdrawal == null) ...[
        const SizedBox(height: 16),
        _withdrawCard(context, d),
      ],
      if (d.history.isNotEmpty) ...[
        const SizedBox(height: 24),
        Text('History', style: text.titleMedium),
        const SizedBox(height: 8),
        FsCard(
          padding: EdgeInsets.zero,
          child: Column(
            children: [
              for (final (index, entry) in d.history.indexed) ...[
                if (index > 0) const Divider(),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(_entryLabel(entry)),
                            Text(ago(entry.createdAt), style: TextStyle(fontSize: 14, color: c.subtle)),
                          ],
                        ),
                      ),
                      Text(
                        '${entry.amount > 0 ? '+' : '−'}${rupees(entry.amount.abs())}',
                        style: TextStyle(
                          fontWeight: FontWeight.w500,
                          fontFeatures: tabular,
                          color: entry.amount < 0 ? c.danger : null,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ],
          ),
        ),
      ],
    ];
  }

  Widget _withdrawCard(BuildContext context, Deposit d) {
    final c = context.fs;
    return FsCard(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text('Take your deposit back', style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 4),
          Text(
            "FutSlot sends all of ${rupees(d.balance)} to you 3 days after you ask, once no game you've booked at a "
            'venue that needs it is still to be played.',
            style: TextStyle(fontSize: 14, color: c.muted),
          ),
          const SizedBox(height: 12),
          if (!_withdrawOpen)
            Align(
              alignment: Alignment.centerLeft,
              child: OutlinedButton(onPressed: () => setState(() => _withdrawOpen = true), child: const Text('Take it back')),
            )
          else ...[
            const FieldLabel('Send it to my'),
            Segmented(
              options: _sendMethods,
              value: _sendMethod,
              onChanged: (method) => setState(() => _sendMethod = method),
            ),
            const SizedBox(height: 12),
            FieldLabel(_sendMethod == PaymentMethod.bankTransfer ? 'Bank name and account number' : 'Number'),
            TextField(
              controller: _sendTo,
              maxLength: 200,
              autocorrect: false,
              keyboardType: _sendMethod == PaymentMethod.bankTransfer ? TextInputType.text : TextInputType.phone,
              style: const TextStyle(fontFeatures: tabular),
              decoration: const InputDecoration(counterText: ''),
            ),
            if (_withdrawError case final error?) ...[const SizedBox(height: 8), Notice(error)],
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: FilledButton(
                    onPressed: _withdrawing ? null : _withdraw,
                    style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(44)),
                    child: Text(_withdrawing ? 'Asking…' : 'Ask for it back'),
                  ),
                ),
                const SizedBox(width: 8),
                TextButton(onPressed: () => setState(() => _withdrawOpen = false), child: const Text('Back')),
              ],
            ),
          ],
        ],
      ),
    );
  }
}
