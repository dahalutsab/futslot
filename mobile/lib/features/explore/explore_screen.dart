import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/api.dart';
import '../../core/api_error.dart';
import '../../core/api_types.dart';
import '../../core/session.dart';
import '../../core/theme.dart';
import '../../l10n/l10n.dart';
import '../../shared/language_switch.dart';
import '../../shared/notification_bell.dart';
import '../../shared/venue_card.dart';
import '../../shared/widgets.dart';

/// Finding a venue: search by name or area, narrow to a city, and featured venues first.
class ExploreScreen extends ConsumerStatefulWidget {
  const ExploreScreen({super.key});

  @override
  ConsumerState<ExploreScreen> createState() => _ExploreScreenState();
}

class _ExploreScreenState extends ConsumerState<ExploreScreen> {
  final _query = TextEditingController();

  /// What was searched; [_query] is what's typed.
  String? _q;

  String? _city;

  List<VenueSummary>? _venues;

  var _featured = <VenueSummary>[];

  var _page = 0;

  var _total = 0;

  var _hasMore = false;

  var _loading = false;

  String? _error;

  /// Cities seen in the unfiltered list, so the chips only offer places that have venues.
  var _knownCities = <String>[];

  bool get _filtered => _q != null || _city != null;

  List<String> get _cities => _city != null && !_knownCities.contains(_city) ? [..._knownCities, _city!] : _knownCities;

  Api get _api => ref.read(apiProvider);

  @override
  void initState() {
    super.initState();
    _load(0);
    _loadFeatured();
  }

  @override
  void dispose() {
    _query.dispose();
    super.dispose();
  }

  Future<void> _loadFeatured() async {
    try {
      final featured = await _api.featured();
      if (mounted) {
        setState(() => _featured = featured);
      }
    } catch (_) {
      // Featured venues are extra; the full list still shows
    }
  }

  void _search() {
    FocusScope.of(context).unfocus();
    final q = _query.text.trim();
    _q = q.isEmpty ? null : q;
    _load(0);
  }

  void _pickCity(String? city) {
    _city = city;
    _load(0);
  }

  Future<void> _load(int page) async {
    setState(() {
      _loading = true;
      _error = null;
      if (page == 0) {
        _venues = null;
      }
    });
    try {
      final result = await _api.venues(q: _q, city: _city, page: page);
      if (!mounted) {
        return;
      }
      setState(() {
        _venues = page == 0 ? result.content : [...?_venues, ...result.content];
        _page = page;
        _total = result.totalElements;
        _hasMore = page + 1 < result.totalPages;
        if (!_filtered) {
          _knownCities = ({..._knownCities, ...result.content.map((venue) => venue.city)}.toList()..sort());
        }
      });
    } catch (error) {
      if (mounted) {
        setState(() => _error = problemOf(error).message);
      }
    } finally {
      if (mounted) {
        setState(() => _loading = false);
      }
    }
  }

  Future<void> _refresh() async {
    await Future.wait([_load(0), _loadFeatured()]);
  }

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    final venues = _venues;
    // Signed-in players switch language in Me
    final signedIn = ref.watch(sessionProvider) != null;
    return Scaffold(
      body: SafeArea(
        bottom: false,
        child: ScreenBody(
          maxWidth: 720,
          onRefresh: _refresh,
          children: [
            Row(
              children: [
                Expanded(child: Text(l.exploreTitle, style: Theme.of(context).textTheme.headlineSmall)),
                if (!signedIn) const LanguageToggle(),
                const NotificationBell(),
              ],
            ),
            const SizedBox(height: 4),
            Text(
              l.exploreSubtitle,
              style: TextStyle(color: c.muted),
            ),
            const SizedBox(height: 20),
            TextField(
              controller: _query,
              textInputAction: TextInputAction.search,
              onSubmitted: (_) => _search(),
              decoration: InputDecoration(
                hintText: l.venueSearchHint,
                prefixIcon: Icon(LucideIcons.search, size: 18, color: c.subtle),
                suffixIcon: _query.text.isEmpty && _q == null
                    ? null
                    : IconButton(
                        tooltip: l.clear,
                        icon: const Icon(LucideIcons.x, size: 18),
                        onPressed: () {
                          _query.clear();
                          _search();
                        },
                      ),
              ),
              onChanged: (_) => setState(() {}),
            ),
            if (_cities.length > 1) ...[
              const SizedBox(height: 12),
              SizedBox(
                height: 36,
                child: ListView(
                  scrollDirection: Axis.horizontal,
                  children: [
                    for (final city in [null, ..._cities])
                      Padding(
                        padding: const EdgeInsets.only(right: 8),
                        child: Pill(label: city ?? l.allCities, selected: _city == city, onTap: () => _pickCity(city)),
                      ),
                  ],
                ),
              ),
            ],
            if (_featured.isNotEmpty && !_filtered) ...[
              const SizedBox(height: 32),
              Text(l.featured, style: Theme.of(context).textTheme.titleLarge),
              const SizedBox(height: 12),
              for (final venue in _featured) Padding(padding: const EdgeInsets.only(bottom: 16), child: VenueCard(venue)),
            ],
            const SizedBox(height: 32),
            Text(
              _filtered ? l.venuesFound(_total) : l.allVenues,
              style: Theme.of(context).textTheme.titleLarge,
            ),
            const SizedBox(height: 12),
            if (_error case final error?)
              ErrorPanel(message: error, onRetry: () => _load(0))
            else if (venues == null)
              for (var i = 0; i < 3; i++) const Padding(padding: EdgeInsets.only(bottom: 16), child: Skeleton(height: 256))
            else if (venues.isEmpty)
              EmptyPanel(title: l.noVenuesFound, body: l.noVenuesFoundBody)
            else ...[
              for (final venue in venues) Padding(padding: const EdgeInsets.only(bottom: 16), child: VenueCard(venue)),
              if (_hasMore)
                Center(
                  child: OutlinedButton(
                    onPressed: _loading ? null : () => _load(_page + 1),
                    child: Text(_loading ? l.loadingMore : l.showMoreVenues),
                  ),
                ),
            ],
          ],
        ),
      ),
    );
  }
}
