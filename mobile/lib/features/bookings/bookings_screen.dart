import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/api.dart';
import '../../core/api_error.dart';
import '../../core/api_types.dart';
import '../../core/theme.dart';
import '../../l10n/l10n.dart';
import '../../shared/booking_model.dart';
import '../../shared/format.dart';
import '../../shared/notification_bell.dart';
import '../../shared/status_chip.dart';
import '../../shared/widgets.dart';

enum _Scope { upcoming, past }

/// The player's bookings everywhere: what's coming up, and what's been played or cancelled.
class BookingsScreen extends ConsumerStatefulWidget {
  const BookingsScreen({super.key});

  @override
  ConsumerState<BookingsScreen> createState() => _BookingsScreenState();
}

class _BookingsScreenState extends ConsumerState<BookingsScreen> {
  var _scope = _Scope.upcoming;

  List<Booking>? _bookings;

  var _page = 0;

  var _hasMore = false;

  var _loading = false;

  String? _error;

  @override
  void initState() {
    super.initState();
    _load(0);
  }

  void _show(_Scope scope) {
    setState(() => _scope = scope);
    _load(0);
  }

  Future<void> _load(int page) async {
    final scope = _scope;
    setState(() {
      _loading = true;
      _error = null;
      if (page == 0) {
        _bookings = null;
      }
    });
    try {
      final list = await ref.read(apiProvider).bookings(scope.name, page: page);
      if (!mounted || scope != _scope) {
        return;
      }
      setState(() {
        _bookings = page == 0 ? list : [...?_bookings, ...list];
        _page = page;
        _hasMore = list.length == Api.bookingsPage;
      });
    } catch (error) {
      if (mounted && scope == _scope) {
        setState(() => _error = problemOf(error).message);
      }
    } finally {
      if (mounted) {
        setState(() => _loading = false);
      }
    }
  }

  Future<void> _open(Booking booking) async {
    await context.push('/bookings/${booking.id}');
    // It may have been paid or cancelled meanwhile
    if (mounted) {
      _load(0);
    }
  }

  @override
  Widget build(BuildContext context) {
    final bookings = _bookings;
    final upcoming = _scope == _Scope.upcoming;
    return Scaffold(
      appBar: AppBar(title: Text(l.yourBookings), actions: const [NotificationBell()]),
      body: ScreenBody(
        maxWidth: 768,
        onRefresh: () => _load(0),
        children: [
          Segmented(
            options: [(_Scope.upcoming, l.upcoming), (_Scope.past, l.past)],
            value: _scope,
            onChanged: _show,
          ),
          const SizedBox(height: 16),
          if (_error case final error?)
            ErrorPanel(message: error, onRetry: () => _load(0))
          else if (bookings == null)
            for (var i = 0; i < 3; i++) const Padding(padding: EdgeInsets.only(bottom: 12), child: Skeleton())
          else if (bookings.isEmpty)
            EmptyPanel(
              title: upcoming ? l.noGamesComingUp : l.nothingHereYet,
              body: upcoming ? l.findCourtPitch : l.pastGamesShowHere,
              action: upcoming ? FilledButton(onPressed: () => context.go('/'), child: const Text('Find a court')) : null,
            )
          else ...[
            for (final booking in bookings)
              Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: _BookingRow(booking, onTap: () => _open(booking)),
              ),
            if (_hasMore)
              Center(
                child: OutlinedButton(
                  onPressed: _loading ? null : () => _load(_page + 1),
                  child: Text(_loading ? l.loadingMore : l.showMore),
                ),
              ),
          ],
        ],
      ),
    );
  }
}

class _BookingRow extends StatelessWidget {
  const _BookingRow(this.booking, {required this.onTap});

  final Booking booking;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    final b = booking;
    final [weekday, day, month] = dayLabel(b.startsAt.substring(0, 10)).split(' ');
    return Material(
      color: c.surface,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12), side: BorderSide(color: c.border)),
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              ExcludeSemantics(
                child: SizedBox(
                  width: 56,
                  child: Column(
                    children: [
                      Text(weekday.toUpperCase(), style: TextStyle(fontSize: 12, fontWeight: FontWeight.w500, color: c.muted)),
                      Text(day, style: const TextStyle(fontSize: 24, fontWeight: FontWeight.w600, height: 1.2, fontFeatures: tabular)),
                      Text(month, style: TextStyle(fontSize: 12, color: c.muted)),
                    ],
                  ),
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(b.venue.name, style: const TextStyle(fontWeight: FontWeight.w500)),
                    Semantics(
                      label: l.bookingSummaryLabel(
                        dayLabel(b.startsAt.substring(0, 10)),
                        localClock(b.startsAt),
                        localClock(b.endsAt),
                        b.court.name,
                      ),
                      excludeSemantics: true,
                      child: Text(
                        '${l.timeRange(localClock(b.startsAt), localClock(b.endsAt))} · ${b.court.name}',
                        style: TextStyle(fontSize: 14, color: c.muted, fontFeatures: tabular),
                      ),
                    ),
                    const SizedBox(height: 8),
                    Wrap(
                      spacing: 8,
                      runSpacing: 4,
                      crossAxisAlignment: WrapCrossAlignment.center,
                      children: [
                        StatusChip(bookingChip(b)),
                        Text(rupees(b.totalAmount), style: TextStyle(fontSize: 14, color: c.muted, fontFeatures: tabular)),
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
