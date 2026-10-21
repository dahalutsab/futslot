import 'dart:math';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../core/api.dart';
import '../../core/api_error.dart';
import '../../core/api_types.dart';
import '../../core/config.dart';
import '../../core/session.dart';
import '../../core/theme.dart';
import '../../l10n/l10n.dart';
import '../../shared/booking_model.dart';
import '../../shared/format.dart';
import '../../shared/labels.dart';
import '../../shared/widgets.dart';
import 'plans.dart';

/// How many days the day strip offers, at most.
const _daysShown = 14;

/// A slot by court and instant, since the waitlist and availability may write the same start with different
/// offsets.
String _slotKey(String courtId, String startsAt) => '$courtId@${DateTime.parse(startsAt).millisecondsSinceEpoch}';

/// One venue: photos and details, free slots by day and court, the booking rules, and reviews.
class VenueScreen extends ConsumerStatefulWidget {
  const VenueScreen({super.key, required this.slug, this.date});

  final String slug;

  /// YYYY-MM-DD from a shared link, so it opens on the same day.
  final String? date;

  @override
  ConsumerState<VenueScreen> createState() => _VenueScreenState();
}

class _VenueScreenState extends ConsumerState<VenueScreen> {
  VenueDetail? _venue;

  Availability? _availability;

  var _reviews = <Review>[];

  String? _error;

  String? _slotsError;

  String? _day;

  var _saved = false;

  /// Booked slots the player is waiting for here.
  var _waiting = <String>{};

  /// What happened when the player last asked to wait for a slot, shown under that court.
  ({String courtId, String text, bool ok})? _waitNote;

  Api get _api => ref.read(apiProvider);

  bool get _signedIn => ref.read(sessionProvider) != null;

  List<String> get _days {
    final venue = _venue;
    if (venue == null) {
      return const [];
    }
    final first = today(venue.timezone);
    final count = min(_daysShown, venue.policy.maxAdvanceBookingDays + 1);
    return [for (var i = 0; i < count; i++) addDays(first, i)];
  }

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _error = null);
    try {
      final venue = await _api.venue(widget.slug);
      if (!mounted) {
        return;
      }
      setState(() => _venue = venue);
      final days = _days;
      _pickDay(_day ?? (days.contains(widget.date) ? widget.date! : days.first));
      final reviews = await _api.reviews(widget.slug);
      if (mounted) {
        setState(() => _reviews = reviews.content);
      }
      if (_signedIn) {
        final results = await Future.wait([_api.favourites(), _api.waitlist()]);
        if (mounted) {
          setState(() {
            _saved = (results[0] as List<VenueSummary>).any((f) => f.id == venue.id);
            _waiting = {
              for (final entry in results[1] as List<WaitlistEntry>)
                if (entry.venueId == venue.id && entry.live) _slotKey(entry.courtId, entry.startsAt),
            };
          });
        }
      }
    } catch (error) {
      if (mounted && _venue == null) {
        setState(() => _error = problemOf(error).message);
      }
    }
  }

  Future<void> _pickDay(String day) async {
    setState(() {
      _day = day;
      _availability = null;
      _slotsError = null;
    });
    try {
      final availability = await _api.availability(widget.slug, day);
      if (mounted && _day == day) {
        setState(() => _availability = availability);
      }
    } catch (error) {
      if (mounted && _day == day) {
        setState(() => _slotsError = problemOf(error).message);
      }
    }
  }

  /// Signing in comes back to this venue.
  void _signIn() =>
      context.push(Uri(path: '/login', queryParameters: {'next': '/venues/${widget.slug}'}).toString());

  Future<void> _toggleSaved(VenueDetail venue) async {
    if (!_signedIn) {
      _signIn();
      return;
    }
    final on = !_saved;
    setState(() => _saved = on);
    try {
      await _api.favourite(venue.id, on);
    } catch (_) {
      if (mounted) {
        setState(() => _saved = !on);
      }
    }
  }

  void _message(VenueDetail venue) {
    context.push(Uri(path: '/messages/new', queryParameters: {'venue': venue.id, 'name': venue.name}).toString());
  }

  /// A booked slot: offer to wait for it.
  Future<void> _askToWait(CourtSlots court, Slot slot) async {
    if (!_signedIn) {
      _signIn();
      return;
    }
    final c = context.fs;
    final wait = await showModalBottomSheet<bool>(
      context: context,
      backgroundColor: c.surface,
      showDragHandle: true,
      builder: (context) => SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(20, 0, 20, 20),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                l.slotBookedOn(localClock(slot.startsAt), court.name),
                style: Theme.of(context).textTheme.titleMedium,
              ),
              const SizedBox(height: 4),
              Text(
                l.waitBody,
                style: TextStyle(color: c.muted),
              ),
              const SizedBox(height: 16),
              FilledButton(onPressed: () => Navigator.pop(context, true), child: Text(l.waitForIt)),
            ],
          ),
        ),
      ),
    );
    if (wait != true || !mounted) {
      return;
    }
    try {
      await _api.joinWaitlist(court.courtId, slot.startsAt);
      if (mounted) {
        setState(() {
          _waiting = {..._waiting, _slotKey(court.courtId, slot.startsAt)};
          _waitNote = (courtId: court.courtId, ok: true, text: l.weWillTellYou(localClock(slot.startsAt)));
        });
      }
    } catch (error) {
      if (mounted) {
        setState(() => _waitNote = (courtId: court.courtId, ok: false, text: problemOf(error).message));
      }
    }
  }

  void _book(CourtSlots court, Slot slot) {
    context.push(Uri(
      path: '/venues/${widget.slug}/book',
      queryParameters: {'court': court.courtId, 'start': slot.startsAt},
    ).toString());
  }

  @override
  Widget build(BuildContext context) {
    final venue = _venue;
    return Scaffold(
      appBar: AppBar(
        title: Text(venue?.name ?? ''),
        actions: [
          if (venue != null) ...[
            IconButton(
              tooltip: _saved ? l.saved_ : l.saveVenue,
              isSelected: _saved,
              onPressed: () => _toggleSaved(venue),
              icon: Icon(LucideIcons.heart, color: context.fs.muted),
              selectedIcon: Icon(Icons.favorite_rounded, color: context.fs.noshow.line),
            ),
            if (ref.watch(sessionProvider) != null)
              IconButton(
                tooltip: l.message,
                onPressed: () => _message(venue),
                icon: Icon(LucideIcons.messageSquare, color: context.fs.muted),
              ),
          ],
        ],
      ),
      body: switch ((venue, _error)) {
        (_, final String error) => ScreenBody(children: [
            ErrorPanel(message: error, onRetry: _load),
          ]),
        (final VenueDetail v, _) => _body(context, v),
        _ => const ScreenBody(children: [Skeleton(height: 200), SizedBox(height: 20), Skeleton(height: 32)]),
      },
    );
  }

  Widget _body(BuildContext context, VenueDetail v) {
    final c = context.fs;
    final text = Theme.of(context).textTheme;
    return ScreenBody(
      maxWidth: 720,
      onRefresh: () async {
        await _load();
      },
      children: [
        if (v.photos.isNotEmpty) ...[_Photos(v.photos, name: v.name), const SizedBox(height: 16)],
        Text(v.name, style: text.headlineSmall),
        const SizedBox(height: 4),
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Padding(padding: const EdgeInsets.only(top: 4), child: Icon(LucideIcons.mapPin, size: 16, color: c.muted)),
            const SizedBox(width: 4),
            Expanded(child: Text(v.address, style: TextStyle(color: c.muted))),
          ],
        ),
        const SizedBox(height: 4),
        Wrap(
          spacing: 16,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            if (v.reviewCount > 0)
              Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(Icons.star_rounded, size: 18, color: c.held.line),
                  const SizedBox(width: 2),
                  Text(
                    digits(v.averageRating.toStringAsFixed(1)),
                    style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w500, fontFeatures: tabular),
                  ),
                  Text(
                    ' · ${l.reviews(v.reviewCount)}',
                    style: TextStyle(fontSize: 14, color: c.muted),
                  ),
                ],
              ),
            if (v.phone case final number?)
              TextButton.icon(
                onPressed: () => launchUrl(Uri(scheme: 'tel', path: number)),
                style: TextButton.styleFrom(foregroundColor: c.muted, padding: EdgeInsets.zero),
                icon: const Icon(LucideIcons.phone, size: 15),
                label: Text(phone(number), style: const TextStyle(fontSize: 14, fontFeatures: tabular)),
              ),
          ],
        ),
        if (v.description case final description?) ...[
          const SizedBox(height: 12),
          Text(description, style: TextStyle(color: c.muted)),
        ],
        const SizedBox(height: 32),
        Text(l.bookASlot, style: text.titleLarge),
        const SizedBox(height: 12),
        DayStrip(days: _days, selected: _day, onPick: _pickDay),
        const SizedBox(height: 4),
        ..._slots(context),
        const SizedBox(height: 32),
        FsCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(l.bookingRules, style: text.titleMedium),
              const SizedBox(height: 8),
              for (final line in policyLines(v.policy)) _Bullet(line),
            ],
          ),
        ),
        const SizedBox(height: 16),
        _Hours(venue: v),
        if (v.amenities.isNotEmpty) ...[
          const SizedBox(height: 16),
          FsCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(l.amenities, style: text.titleMedium),
                const SizedBox(height: 8),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: [
                    for (final amenity in v.amenities)
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                        decoration: BoxDecoration(color: c.surface2, borderRadius: BorderRadius.circular(999)),
                        child: Text(amenityLabel(amenity), style: const TextStyle(fontSize: 14)),
                      ),
                  ],
                ),
              ],
            ),
          ),
        ],
        VenuePlans(venue: v),
        const SizedBox(height: 32),
        Text(l.reviewsTitle, style: text.titleLarge),
        const SizedBox(height: 8),
        if (_reviews.isEmpty)
          Text(l.noReviewsYet, style: TextStyle(color: c.muted))
        else ...[
          Text(
            l.ratingFrom(digits(v.averageRating.toStringAsFixed(1)), l.players(v.reviewCount)),
            style: TextStyle(color: c.muted),
          ),
          const SizedBox(height: 12),
          for (final review in _reviews)
            Padding(padding: const EdgeInsets.only(bottom: 12), child: _ReviewCard(review, venueName: v.name)),
        ],
      ],
    );
  }

  List<Widget> _slots(BuildContext context) {
    final c = context.fs;
    if (_slotsError case final error?) {
      return [const SizedBox(height: 12), Notice(error)];
    }
    final availability = _availability;
    if (availability == null) {
      return [const SizedBox(height: 12), const Skeleton(height: 160)];
    }
    if (availability.courts.isEmpty) {
      return [const SizedBox(height: 12), Text(l.noCourtsYet, style: TextStyle(color: c.muted))];
    }
    return [
      for (final court in availability.courts) ...[
        const SizedBox(height: 16),
        _CourtHeading(court),
        const SizedBox(height: 8),
        if (court.slots.isEmpty)
          Text(l.closedThisDay, style: TextStyle(fontSize: 14, color: c.muted))
        else if (court.slots.every((slot) => slot.status == SlotStatus.past))
          Text(l.noMoreGamesToday, style: TextStyle(fontSize: 14, color: c.muted))
        else
          GridView.count(
            crossAxisCount: MediaQuery.sizeOf(context).width >= 600 ? 5 : 3,
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            mainAxisSpacing: 8,
            crossAxisSpacing: 8,
            childAspectRatio: 1.7,
            children: [
              // Past slots only clutter today's list
              for (final slot in court.slots.where((slot) => slot.status != SlotStatus.past))
                _SlotTile(
                  slot: slot,
                  waiting: _waiting.contains(_slotKey(court.courtId, slot.startsAt)),
                  onBook: () => _book(court, slot),
                  onWait: () => _askToWait(court, slot),
                ),
            ],
          ),
        if (_waitNote case final note? when note.courtId == court.courtId) ...[
          const SizedBox(height: 8),
          Notice(note.text, color: note.ok ? c.confirmed.fg : c.danger),
        ],
      ],
    ];
  }
}

class _Photos extends StatefulWidget {
  const _Photos(this.photos, {required this.name});

  final List<VenuePhoto> photos;
  final String name;

  @override
  State<_Photos> createState() => _PhotosState();
}

class _PhotosState extends State<_Photos> {
  var _index = 0;

  @override
  Widget build(BuildContext context) {
    final photos = widget.photos;
    return ClipRRect(
      borderRadius: BorderRadius.circular(12),
      child: AspectRatio(
        aspectRatio: 16 / 9,
        child: Stack(
          children: [
            PageView.builder(
              itemCount: photos.length,
              onPageChanged: (index) => setState(() => _index = index),
              itemBuilder: (context, index) => ColoredBox(
                color: context.fs.surface2,
                child: Image.network(
                  mediaUrl(photos[index].url),
                  fit: BoxFit.cover,
                  semanticLabel: photos[index].caption ?? widget.name,
                  errorBuilder: (_, _, _) => const SizedBox.shrink(),
                ),
              ),
            ),
            if (photos.length > 1)
              Positioned(
                left: 0,
                right: 0,
                bottom: 10,
                child: ExcludeSemantics(
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      for (var i = 0; i < photos.length; i++)
                        Container(
                          width: 8,
                          height: 8,
                          margin: const EdgeInsets.symmetric(horizontal: 4),
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            color: Colors.white.withValues(alpha: i == _index ? 1 : 0.5),
                            boxShadow: const [BoxShadow(color: Colors.black26, blurRadius: 2)],
                          ),
                        ),
                    ],
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _CourtHeading extends StatelessWidget {
  const _CourtHeading(this.court);

  final CourtSlots court;

  @override
  Widget build(BuildContext context) {
    final free = court.slots.where((slot) => slot.status == SlotStatus.available).length;
    return Text.rich(
      TextSpan(
        text: court.name,
        style: const TextStyle(fontWeight: FontWeight.w500),
        children: [
          TextSpan(
            text: ' · ${free == 0 ? l.fullyBooked : l.freeSlots(free)}',
            style: TextStyle(fontWeight: FontWeight.w400, color: context.fs.muted),
          ),
        ],
      ),
    );
  }
}

class _SlotTile extends StatelessWidget {
  const _SlotTile({required this.slot, required this.waiting, required this.onBook, required this.onWait});

  final Slot slot;

  /// The player is on the waitlist for it.
  final bool waiting;
  final VoidCallback onBook;
  final VoidCallback onWait;

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    final time = localClock(slot.startsAt);
    if (slot.status == SlotStatus.booked && waiting) {
      return Semantics(
        label: l.slotWaiting(time),
        excludeSemantics: true,
        child: Container(
          decoration: BoxDecoration(
            color: c.held.bg,
            border: Border.all(color: c.held.line),
            borderRadius: BorderRadius.circular(8),
          ),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(time, style: TextStyle(fontWeight: FontWeight.w500, color: c.held.fg, fontFeatures: tabular)),
              Text(l.waiting, style: TextStyle(fontSize: 12, color: c.held.fg)),
            ],
          ),
        ),
      );
    }
    if (slot.status == SlotStatus.booked) {
      return Semantics(
        label: l.slotWaitQuestion(time),
        button: true,
        // Excluding the InkWell's semantics drops its tap, so screen readers act through this
        onTap: onWait,
        excludeSemantics: true,
        child: Material(
          color: c.surface2,
          borderRadius: BorderRadius.circular(8),
          child: InkWell(
            borderRadius: BorderRadius.circular(8),
            onTap: onWait,
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  time,
                  style: TextStyle(
                    fontWeight: FontWeight.w500,
                    color: c.subtle,
                    decoration: TextDecoration.lineThrough,
                    fontFeatures: tabular,
                  ),
                ),
                Text(l.booked, style: TextStyle(fontSize: 12, color: c.subtle)),
              ],
            ),
          ),
        ),
      );
    }
    if (slot.status != SlotStatus.available) {
      final why = l.closed;
      return Semantics(
        label: l.slotClosed(time),
        excludeSemantics: true,
        child: Container(
          decoration: BoxDecoration(color: c.surface2, borderRadius: BorderRadius.circular(8)),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(
                time,
                style: TextStyle(
                  fontWeight: FontWeight.w500,
                  color: c.subtle,
                  decoration: TextDecoration.lineThrough,
                  fontFeatures: tabular,
                ),
              ),
              Text(why, style: TextStyle(fontSize: 12, color: c.subtle)),
            ],
          ),
        ),
      );
    }
    return Semantics(
      label: l.slotBook(time, rupees(slot.price)),
      button: true,
      onTap: onBook,
      excludeSemantics: true,
      child: Material(
        color: c.surface,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8), side: BorderSide(color: c.border)),
        child: InkWell(
          borderRadius: BorderRadius.circular(8),
          onTap: onBook,
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(time, style: const TextStyle(fontWeight: FontWeight.w500, fontFeatures: tabular)),
              Text(rupees(slot.price), style: TextStyle(fontSize: 12, color: c.muted, fontFeatures: tabular)),
            ],
          ),
        ),
      ),
    );
  }
}

class _Bullet extends StatelessWidget {
  const _Bullet(this.text);

  final String text;

  @override
  Widget build(BuildContext context) {
    final style = TextStyle(fontSize: 14, color: context.fs.muted, height: 1.45);
    return Padding(
      padding: const EdgeInsets.only(bottom: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('•  ', style: style),
          Expanded(child: Text(text, style: style)),
        ],
      ),
    );
  }
}

class _Hours extends StatelessWidget {
  const _Hours({required this.venue});

  final VenueDetail venue;

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    // DateTime.weekday is 1 (Monday) to 7 (Sunday); the list starts on Sunday
    final todayIndex = weekdayOf(today(venue.timezone)) % 7;
    return FsCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(l.openingHours, style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 8),
          for (final (index, day) in week.indexed)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 2),
              child: DefaultTextStyle.merge(
                style: TextStyle(fontSize: 14, fontWeight: index == todayIndex ? FontWeight.w600 : FontWeight.w400),
                child: Row(
                  children: [
                    Expanded(
                      child: Text(day.label, style: TextStyle(color: index == todayIndex ? null : c.muted)),
                    ),
                    Text(
                      hoursLabel(venue.operatingHours.where((hours) => hours.day == day.day).firstOrNull),
                      style: const TextStyle(fontFeatures: tabular),
                    ),
                  ],
                ),
              ),
            ),
        ],
      ),
    );
  }
}

class _ReviewCard extends StatelessWidget {
  const _ReviewCard(this.review, {required this.venueName});

  final Review review;
  final String venueName;

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    return FsCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(child: Text(review.reviewerName, style: const TextStyle(fontWeight: FontWeight.w500))),
              Semantics(
                label: l.starsOutOfFive(digits(review.rating)),
                excludeSemantics: true,
                child: Text(
                  '★' * review.rating + '☆' * (5 - review.rating),
                  style: TextStyle(color: c.held.line),
                ),
              ),
            ],
          ),
          if (review.comment case final comment?) ...[const SizedBox(height: 8), Text(comment)],
          const SizedBox(height: 8),
          Text(l.playedOn(dayLabel(review.playedOn)), style: TextStyle(fontSize: 14, color: c.subtle)),
          if (review.ownerReply case final reply?) ...[
            const SizedBox(height: 12),
            Container(
              width: double.infinity,
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              decoration: BoxDecoration(color: c.surface2, borderRadius: BorderRadius.circular(8)),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(l.venueReplied(venueName), style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w500)),
                  const SizedBox(height: 2),
                  Text(reply, style: TextStyle(fontSize: 14, color: c.muted)),
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }
}
