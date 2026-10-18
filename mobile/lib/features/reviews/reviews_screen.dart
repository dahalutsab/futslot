import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/api.dart';
import '../../core/api_error.dart';
import '../../core/api_types.dart';
import '../../core/theme.dart';
import '../../shared/format.dart';
import '../../shared/review_form.dart';
import '../../shared/widgets.dart';

/// Games waiting for a review, and the reviews the player wrote (each edited from its booking).
class ReviewsScreen extends ConsumerStatefulWidget {
  const ReviewsScreen({super.key});

  @override
  ConsumerState<ReviewsScreen> createState() => _ReviewsScreenState();
}

class _ReviewsScreenState extends ConsumerState<ReviewsScreen> {
  var _toReview = <Booking>[];

  List<Review>? _mine;

  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final api = ref.read(apiProvider);
    try {
      final results = await Future.wait([api.toReview(), api.myReviews()]);
      if (mounted) {
        setState(() {
          _toReview = results[0] as List<Booking>;
          _mine = (results[1] as Paged<Review>).content;
          _error = null;
        });
      }
    } catch (error) {
      if (mounted) {
        setState(() => _error = problemOf(error).message);
      }
    }
  }

  Future<void> _openBooking(String id) async {
    await context.push('/bookings/$id?review=1');
    // A review may have been written or changed there
    _load();
  }

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    final text = Theme.of(context).textTheme;
    final mine = _mine;
    return Scaffold(
      appBar: AppBar(title: const Text('Reviews')),
      body: ScreenBody(
        onRefresh: _load,
        children: [
          if (_error case final error?) ...[Notice(error), const SizedBox(height: 16)],
          if (_toReview.isNotEmpty) ...[
            Text('How was your game?', style: text.titleMedium),
            Text('You can review a game for 30 days after playing it.', style: TextStyle(fontSize: 14, color: c.muted)),
            const SizedBox(height: 12),
            for (final booking in _toReview)
              Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: Material(
                  color: c.surface,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12), side: BorderSide(color: c.border)),
                  child: InkWell(
                    borderRadius: BorderRadius.circular(12),
                    onTap: () => _openBooking(booking.id),
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Row(
                        children: [
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(booking.venue.name, style: const TextStyle(fontWeight: FontWeight.w500)),
                                Text(
                                  '${dayLabel(booking.startsAt.substring(0, 10))} · ${localClock(booking.startsAt)}',
                                  style: TextStyle(fontSize: 14, color: c.muted, fontFeatures: tabular),
                                ),
                              ],
                            ),
                          ),
                          const Text('Review', style: TextStyle(fontSize: 14, fontWeight: FontWeight.w500)),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            const SizedBox(height: 24),
          ],
          Text('Your reviews', style: text.titleMedium),
          const SizedBox(height: 8),
          if (mine == null)
            if (_error == null) const Skeleton(height: 128) else const SizedBox.shrink()
          else if (mine.isEmpty)
            Text("You haven't reviewed a venue yet.", style: TextStyle(color: c.muted))
          else
            for (final review in mine)
              Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: FsCard(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Expanded(child: Text(review.venueName, style: const TextStyle(fontWeight: FontWeight.w500))),
                          Stars(review.rating),
                        ],
                      ),
                      if (review.comment case final comment?) ...[const SizedBox(height: 8), Text(comment)],
                      if (review.status != ReviewStatus.published) ...[
                        const SizedBox(height: 8),
                        Text(
                          review.status == ReviewStatus.hidden ? 'Hidden by FutSlot while it checks it' : 'Removed by FutSlot',
                          style: TextStyle(fontSize: 14, color: c.noshow.fg),
                        ),
                      ],
                      if (review.ownerReply case final reply?) ...[
                        const SizedBox(height: 12),
                        Container(
                          width: double.infinity,
                          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                          decoration: BoxDecoration(color: c.surface2, borderRadius: BorderRadius.circular(8)),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text('${review.venueName} replied',
                                  style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w500)),
                              Text(reply, style: TextStyle(fontSize: 14, color: c.muted)),
                            ],
                          ),
                        ),
                      ],
                      const SizedBox(height: 4),
                      Row(
                        children: [
                          Text('Played ${dayLabel(review.playedOn)}', style: TextStyle(fontSize: 14, color: c.subtle)),
                          if (review.bookingId case final bookingId? when review.status != ReviewStatus.removed) ...[
                            const SizedBox(width: 8),
                            TextButton(
                              onPressed: () => _openBooking(bookingId),
                              child: const Text('Edit', style: TextStyle(fontSize: 14)),
                            ),
                          ],
                        ],
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

