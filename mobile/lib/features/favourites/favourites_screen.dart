import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/api.dart';
import '../../core/api_error.dart';
import '../../core/api_types.dart';
import '../../l10n/l10n.dart';
import '../../shared/venue_card.dart';
import '../../shared/widgets.dart';

/// Venues the player saved, to book again quickly.
class FavouritesScreen extends ConsumerStatefulWidget {
  const FavouritesScreen({super.key});

  @override
  ConsumerState<FavouritesScreen> createState() => _FavouritesScreenState();
}

class _FavouritesScreenState extends ConsumerState<FavouritesScreen> {
  List<VenueSummary>? _venues;

  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final venues = await ref.read(apiProvider).favourites();
      if (mounted) {
        setState(() {
          _venues = venues;
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
    final venues = _venues;
    return Scaffold(
      appBar: AppBar(title: Text(l.savedVenues)),
      body: ScreenBody(
        maxWidth: 720,
        onRefresh: _load,
        children: [
          if (_error case final error?)
            ErrorPanel(message: error, onRetry: _load)
          else if (venues == null)
            const Skeleton(height: 256)
          else if (venues.isEmpty)
            EmptyPanel(
              title: l.noSavedVenues,
              body: l.noSavedVenuesBody,
              action: FilledButton(onPressed: () => context.go('/'), child: const Text('Find a court')),
            )
          else
            for (final venue in venues) Padding(padding: const EdgeInsets.only(bottom: 16), child: VenueCard(venue)),
        ],
      ),
    );
  }
}
