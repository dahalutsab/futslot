import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../core/api_types.dart';
import '../core/config.dart';
import '../core/theme.dart';
import 'format.dart';

/// A venue in a list: photo, where it is, rating and the lowest price.
class VenueCard extends StatelessWidget {
  const VenueCard(this.venue, {super.key});

  final VenueSummary venue;

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    final v = venue;
    return Material(
      color: c.surface,
      clipBehavior: Clip.antiAlias,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12), side: BorderSide(color: c.border)),
      child: InkWell(
        onTap: () => context.push('/venues/${v.slug}'),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            AspectRatio(
              aspectRatio: 16 / 9,
              child: ColoredBox(
                color: c.surface2,
                child: v.coverPhotoUrl == null
                    ? _Initial(v.name)
                    : Image.network(
                        mediaUrl(v.coverPhotoUrl!),
                        fit: BoxFit.cover,
                        semanticLabel: '',
                        errorBuilder: (_, _, _) => _Initial(v.name),
                      ),
              ),
            ),
            Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Expanded(child: Text(v.name, style: const TextStyle(fontWeight: FontWeight.w600, height: 1.3))),
                      if (v.reviewCount > 0) ...[
                        const SizedBox(width: 12),
                        _Rating(rating: v.averageRating, count: v.reviewCount),
                      ],
                    ],
                  ),
                  const SizedBox(height: 4),
                  Row(
                    children: [
                      Icon(LucideIcons.mapPin, size: 14, color: c.muted),
                      const SizedBox(width: 4),
                      Expanded(
                        child: Text(
                          '${v.area != null ? '${v.area}, ' : ''}${v.city}',
                          style: TextStyle(fontSize: 14, color: c.muted),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Text.rich(
                    TextSpan(
                      style: TextStyle(fontSize: 14, color: c.muted),
                      children: [
                        if (v.fromPrice != null) ...[
                          const TextSpan(text: 'From '),
                          TextSpan(
                            text: rupees(v.fromPrice),
                            style: TextStyle(fontWeight: FontWeight.w600, color: c.text, fontFeatures: tabular),
                          ),
                          const TextSpan(text: ' · '),
                        ],
                        TextSpan(text: '${v.courtCount} ${v.courtCount == 1 ? 'court' : 'courts'}'),
                      ],
                    ),
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

class _Initial extends StatelessWidget {
  const _Initial(this.name);

  final String name;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: ExcludeSemantics(
        child: Text(
          name.isEmpty ? '' : name[0],
          style: TextStyle(fontSize: 30, fontWeight: FontWeight.w700, color: context.fs.subtle),
        ),
      ),
    );
  }
}

class _Rating extends StatelessWidget {
  const _Rating({required this.rating, required this.count});

  final double rating;
  final int count;

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    return Semantics(
      label: '${rating.toStringAsFixed(1)} out of 5 from $count reviews',
      excludeSemantics: true,
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.star_rounded, size: 16, color: c.held.line),
          const SizedBox(width: 2),
          Text(
            rating.toStringAsFixed(1),
            style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w500, fontFeatures: tabular),
          ),
          Text(' ($count)', style: TextStyle(fontSize: 14, color: c.subtle)),
        ],
      ),
    );
  }
}
