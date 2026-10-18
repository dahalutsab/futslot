import 'dart:async';
import 'dart:math';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/api.dart';
import '../../core/api_error.dart';
import '../../core/api_types.dart';
import '../../core/theme.dart';
import '../../shared/booking_model.dart';
import '../../shared/format.dart';
import '../../shared/pay_form.dart';
import '../../shared/status_chip.dart';
import '../../shared/widgets.dart';

/// One booking: when and where, paying the advance, the money so far, and cancelling.
class BookingScreen extends ConsumerStatefulWidget {
  const BookingScreen({super.key, required this.id, this.justBooked = false});

  final String id;

  /// The player has just booked it.
  final bool justBooked;

  @override
  ConsumerState<BookingScreen> createState() => _BookingScreenState();
}

class _BookingScreenState extends ConsumerState<BookingScreen> {
  final _reason = TextEditingController();

  Booking? _booking;

  BookingPayments? _payments;

  VenueDetail? _venue;

  String? _error;

  String? _payError;

  var _paying = false;

  var _cancelOpen = false;

  var _cancelling = false;

  String? _cancelError;

  /// Ticks every 30 seconds, so the time left to pay and the cancel fee stay current.
  var _now = DateTime.now();

  late final Timer _timer;

  Api get _api => ref.read(apiProvider);

  /// A payment the venue hasn't checked yet.
  bool get _checking => _payments?.payments.any((p) => p.status == TransactionStatus.pending) ?? false;

  bool get _canPay {
    final booking = _booking;
    final until = booking?.holdExpiresAt;
    return booking?.status == BookingStatus.pendingPayment &&
        !_checking &&
        until != null &&
        DateTime.parse(until).isAfter(_now);
  }

  int get _minutesLeft {
    final until = _booking?.holdExpiresAt;
    return until == null ? 0 : max(0, (DateTime.parse(until).difference(_now).inSeconds / 60).ceil());
  }

  @override
  void initState() {
    super.initState();
    _timer = Timer.periodic(const Duration(seconds: 30), (_) => setState(() => _now = DateTime.now()));
    _load();
  }

  @override
  void dispose() {
    _timer.cancel();
    _reason.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    try {
      final results = await Future.wait([_api.booking(widget.id), _api.bookingPayments(widget.id)]);
      final booking = results[0] as Booking;
      final payments = results[1] as BookingPayments;
      if (!mounted) {
        return;
      }
      setState(() {
        _booking = booking;
        _payments = payments;
        _now = DateTime.now();
        _error = null;
      });
      if (_venue?.id != booking.venue.id && booking.venue.slug != null) {
        final venue = await _api.venue(booking.venue.slug!);
        if (mounted) {
          setState(() => _venue = venue);
        }
      }
    } catch (error) {
      if (mounted && _booking == null) {
        setState(() => _error = problemOf(error).message);
      }
    }
  }

  Future<void> _pay(PaymentClaim claim) async {
    final booking = _booking;
    if (booking == null || _paying) {
      return;
    }
    setState(() {
      _paying = true;
      _payError = null;
    });
    try {
      await _api.payBooking(booking.id, claim);
      await _load();
    } catch (error) {
      if (mounted) {
        setState(() => _payError = problemOf(error).message);
      }
    } finally {
      if (mounted) {
        setState(() => _paying = false);
      }
    }
  }

  Future<void> _cancel() async {
    final booking = _booking;
    if (booking == null || _cancelling) {
      return;
    }
    setState(() {
      _cancelling = true;
      _cancelError = null;
    });
    try {
      final cancelled = await _api.cancel(booking.id, _reason.text.trim());
      final payments = await _api.bookingPayments(booking.id);
      if (mounted) {
        setState(() {
          _booking = cancelled;
          _payments = payments;
          _cancelOpen = false;
        });
      }
    } catch (error) {
      if (mounted) {
        setState(() => _cancelError = problemOf(error).message);
      }
    } finally {
      if (mounted) {
        setState(() => _cancelling = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final booking = _booking;
    return Scaffold(
      appBar: AppBar(title: Text(booking != null ? 'Booking ${booking.reference}' : 'Booking')),
      body: switch ((booking, _error)) {
        (_, final String error) => ScreenBody(children: [ErrorPanel(message: error, onRetry: _load)]),
        (final Booking b, _) => ScreenBody(onRefresh: _load, children: _body(context, b)),
        _ => const ScreenBody(children: [Skeleton(height: 192)]),
      },
    );
  }

  List<Widget> _body(BuildContext context, Booking b) {
    final c = context.fs;
    final text = Theme.of(context).textTheme;
    final venue = _venue;
    final live = b.status == BookingStatus.confirmed || b.status == BookingStatus.pendingPayment;
    return [
      if (widget.justBooked && b.status != BookingStatus.cancelled) ...[
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          decoration: BoxDecoration(color: c.confirmed.bg, borderRadius: BorderRadius.circular(8)),
          child: Row(
            children: [
              Icon(LucideIcons.circleCheck, size: 18, color: c.confirmed.fg),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  b.status == BookingStatus.confirmed
                      ? 'Booked. See you on the court!'
                      : 'Booked. Pay the advance below to confirm it.',
                  style: TextStyle(color: c.confirmed.fg),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
      ],
      FsCard(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            StatusChip(bookingChip(b, checking: _checking)),
            const SizedBox(height: 12),
            Text(
              '${localClock(b.startsAt)} – ${localClock(b.endsAt)}',
              style: text.headlineSmall?.copyWith(fontFeatures: tabular),
            ),
            const SizedBox(height: 4),
            Text(dayLabel(b.startsAt.substring(0, 10)), style: const TextStyle(fontSize: 18)),
            const SizedBox(height: 12),
            InkWell(
              onTap: b.venue.slug == null ? null : () => context.push('/venues/${b.venue.slug}'),
              child: Text.rich(
                TextSpan(
                  text: b.venue.name,
                  style: const TextStyle(fontWeight: FontWeight.w500),
                  children: [TextSpan(text: ' · ${b.court.name}', style: TextStyle(fontWeight: FontWeight.w400, color: c.muted))],
                ),
              ),
            ),
            if (venue != null) ...[
              const SizedBox(height: 4),
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Padding(padding: const EdgeInsets.only(top: 3), child: Icon(LucideIcons.mapPin, size: 14, color: c.muted)),
                  const SizedBox(width: 4),
                  Expanded(child: Text(venue.address, style: TextStyle(fontSize: 14, color: c.muted))),
                ],
              ),
            ],
          ],
        ),
      ),
      if (b.status == BookingStatus.pendingPayment) ...[const SizedBox(height: 16), _payment(context, b)],
      const SizedBox(height: 16),
      _money(context, b),
      if (b.notes case final notes?) ...[
        const SizedBox(height: 16),
        FsCard(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Your note', style: text.titleMedium),
              const SizedBox(height: 4),
              Text(notes, style: TextStyle(color: c.muted)),
            ],
          ),
        ),
      ],
      if (b.status == BookingStatus.cancelled) ...[
        const SizedBox(height: 16),
        FsCard(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Cancelled', style: text.titleMedium),
              const SizedBox(height: 4),
              Text(
                b.cancellationFeePercent > 0
                    ? "Cancelled late, so the venue kept its ${percent(b.cancellationFeePercent)}% fee"
                        "${b.depositBacked ? ", with any part your advance didn't cover taken from your deposit" : ''}."
                    : 'Anything you paid comes back to you.',
                style: TextStyle(color: c.muted),
              ),
              if (b.cancellationReason case final reason?) ...[
                const SizedBox(height: 4),
                Text('“$reason”', style: TextStyle(color: c.muted)),
              ],
            ],
          ),
        ),
      ],
      if (live && cancellable(b, _now)) ...[const SizedBox(height: 16), _cancelSection(context, b)],
    ];
  }

  Widget _payment(BuildContext context, Booking b) {
    final c = context.fs;
    final text = Theme.of(context).textTheme;
    if (_checking) {
      return FsCard(
        padding: const EdgeInsets.all(20),
        color: c.verify.bg,
        borderColor: c.verify.line,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(LucideIcons.wallet, size: 18, color: c.verify.fg),
                const SizedBox(width: 8),
                Text('Payment sent', style: text.titleMedium?.copyWith(color: c.verify.fg)),
              ],
            ),
            const SizedBox(height: 4),
            Text(
              "${b.venue.name} is checking it. Your slot is held meanwhile, and we'll let you know when it's confirmed.",
              style: TextStyle(color: c.verify.fg),
            ),
          ],
        ),
      );
    }
    if (_canPay) {
      final minutes = _minutesLeft;
      return FsCard(
        padding: const EdgeInsets.all(20),
        borderColor: c.held.line,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text('Pay ${rupees(b.advanceRequired - b.paidAmount)} to confirm', style: text.titleMedium),
            const SizedBox(height: 4),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Padding(padding: const EdgeInsets.only(top: 3), child: Icon(LucideIcons.clock, size: 16, color: c.held.fg)),
                const SizedBox(width: 6),
                Expanded(
                  child: Text(
                    'Held for $minutes more ${minutes == 1 ? 'minute' : 'minutes'}, until '
                    '${clockAt(b.holdExpiresAt!, _venue?.timezone ?? 'Asia/Kathmandu')}.',
                    style: TextStyle(color: c.held.fg),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            PayForm(
              amount: b.totalAmount - b.paidAmount,
              minimum: b.advanceRequired - b.paidAmount,
              instructions: _payments?.paymentInstructions ?? _venue?.paymentInstructions,
              busy: _paying,
              error: _payError,
              label: "I've paid",
              onPaid: _pay,
            ),
            const SizedBox(height: 12),
            Text(
              'Pay the venue directly, then tell us the transaction ID. The venue checks it and confirms your booking.',
              style: TextStyle(fontSize: 14, color: c.subtle),
            ),
          ],
        ),
      );
    }
    return FsCard(
      padding: const EdgeInsets.all(20),
      borderColor: c.held.line,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('The time to pay has run out', style: text.titleMedium),
          const SizedBox(height: 4),
          Text("The slot may have gone to someone else. Book again if it's still free.", style: TextStyle(color: c.muted)),
          if (b.venue.slug != null) ...[
            const SizedBox(height: 12),
            FilledButton(
              onPressed: () => context.push(Uri(
                path: '/venues/${b.venue.slug}',
                queryParameters: {'date': b.startsAt.substring(0, 10)},
              ).toString()),
              child: const Text('See free slots'),
            ),
          ],
        ],
      ),
    );
  }

  Widget _money(BuildContext context, Booking b) {
    final c = context.fs;
    final payments = _payments;
    return FsCard(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Payment', style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 8),
          AmountRow(label: 'Court', value: rupees(b.priceAmount)),
          for (final line in b.addOns)
            AmountRow(label: '${line.quantity} × ${line.name}', value: rupees(line.quantity * line.unitPrice)),
          if (b.discountAmount > 0)
            AmountRow(label: 'Discount', value: '−${rupees(b.discountAmount)}', valueColor: c.confirmed.fg),
          const Padding(padding: EdgeInsets.symmetric(vertical: 6), child: Divider()),
          AmountRow(label: 'Total', value: rupees(b.totalAmount), strong: true),
          AmountRow(label: 'Paid', value: rupees(b.paidAmount)),
          if (b.status == BookingStatus.confirmed || b.status == BookingStatus.pendingPayment)
            AmountRow(label: 'At the venue', value: rupees(max(0, b.totalAmount - b.paidAmount))),
          if (payments != null && payments.payments.isNotEmpty) ...[
            const Padding(padding: EdgeInsets.symmetric(vertical: 8), child: Divider()),
            for (final payment in payments.payments)
              _MoneyLine(
                label: '${methodLabel(payment.method)}${switch (payment.status) {
                  TransactionStatus.pending => ' · being checked',
                  TransactionStatus.initiated => ' · started',
                  TransactionStatus.failed => ' · not received',
                  TransactionStatus.succeeded => '',
                  TransactionStatus.cancelled => ' · cancelled',
                  TransactionStatus.refunded => ' · refunded',
                  TransactionStatus.partiallyRefunded => ' · partly refunded',
                }}',
                detail: payment.failureReason,
                amount: payment.amount,
              ),
          ],
          if (payments != null && payments.refunds.isNotEmpty) ...[
            const Padding(padding: EdgeInsets.symmetric(vertical: 8), child: Divider()),
            for (final refund in payments.refunds)
              _MoneyLine(
                label: 'Refund by ${methodLabel(refund.originalMethod)} · ${refund.sent ? 'sent' : 'on its way'}',
                amount: refund.amount,
              ),
          ],
        ],
      ),
    );
  }

  Widget _cancelSection(BuildContext context, Booking b) {
    final c = context.fs;
    if (!_cancelOpen) {
      return TextButton(
        onPressed: () => setState(() => _cancelOpen = true),
        style: TextButton.styleFrom(foregroundColor: c.danger, minimumSize: const Size.fromHeight(48)),
        child: const Text('Cancel booking'),
      );
    }
    final venue = _venue;
    final outcome = venue == null ? null : cancelOutcome(b, venue.policy, _now);
    return FsCard(
      padding: const EdgeInsets.all(20),
      color: c.noshow.bg,
      borderColor: c.noshow.line,
      child: DefaultTextStyle.merge(
        style: TextStyle(color: c.noshow.fg),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Text('Cancel this booking?', style: TextStyle(fontWeight: FontWeight.w600)),
            if (outcome != null) ...[
              const SizedBox(height: 4),
              Text(_outcomeText(b, outcome)),
            ],
            const SizedBox(height: 12),
            const FieldLabel('Why?', optional: true),
            TextField(controller: _reason, maxLength: 500, decoration: const InputDecoration(counterText: '')),
            if (_cancelError case final error?) ...[
              const SizedBox(height: 8),
              Notice(error, color: c.noshow.fg),
            ],
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: FilledButton(
                    onPressed: _cancelling ? null : _cancel,
                    style: FilledButton.styleFrom(
                      backgroundColor: c.danger,
                      // The dark theme's danger is a light red, which white text doesn't read on
                      foregroundColor: Theme.of(context).brightness == Brightness.dark ? c.inkFg : Colors.white,
                      minimumSize: const Size.fromHeight(44),
                    ),
                    child: Text(_cancelling ? 'Cancelling…' : 'Cancel booking'),
                  ),
                ),
                const SizedBox(width: 8),
                FilledButton(
                  onPressed: () => setState(() => _cancelOpen = false),
                  style: FilledButton.styleFrom(
                    backgroundColor: c.surface,
                    foregroundColor: c.text,
                    minimumSize: const Size(44, 44),
                  ),
                  child: const Text('Keep it'),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  static String _outcomeText(Booking b, CancelOutcome o) {
    if (o.feePercent == 0) {
      return "It's free to cancel now.${b.paidAmount > 0 ? ' You get ${rupees(o.refund)} back.' : ''}";
    }
    return [
      'Cancelling now costs ${percent(o.feePercent)}% of the price (${rupees(o.fee)}).',
      if (o.kept > 0)
        'The venue keeps ${rupees(o.kept)} of what you paid${o.refund > 0 ? ' and you get ${rupees(o.refund)} back' : ''}.',
      if (o.fromDeposit > 0) 'Up to ${rupees(o.fromDeposit)} more comes out of your FutSlot deposit.',
    ].join(' ');
  }
}

class _MoneyLine extends StatelessWidget {
  const _MoneyLine({required this.label, required this.amount, this.detail});

  final String label;
  final String? detail;
  final double amount;

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(label, style: TextStyle(fontSize: 14, color: c.muted)),
                if (detail case final detail?) Text(detail, style: TextStyle(fontSize: 14, color: c.danger)),
              ],
            ),
          ),
          const SizedBox(width: 12),
          Text(rupees(amount), style: const TextStyle(fontSize: 14, fontFeatures: tabular)),
        ],
      ),
    );
  }
}
