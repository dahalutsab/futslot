import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/api.dart';
import '../../core/api_error.dart';
import '../../core/api_types.dart';
import '../../core/theme.dart';
import '../../l10n/l10n.dart';
import '../../shared/format.dart';
import '../../shared/labels.dart';
import '../../shared/widgets.dart';

/// Nepal's offset, for a time picked without one.
const _nepalOffset = '+05:45';

/// Posting a game: short of players for one of your bookings, or your team looking for an opponent. Where and when
/// come from the booking, or when nothing's booked yet, from a venue (or just a city) and a time.
class NewMatchScreen extends ConsumerStatefulWidget {
  const NewMatchScreen({super.key});

  @override
  ConsumerState<NewMatchScreen> createState() => _NewMatchScreenState();
}

class _NewMatchScreenState extends ConsumerState<NewMatchScreen> {
  var _type = MatchPostType.needPlayers;

  var _bookings = <Booking>[];

  /// Teams the player leads.
  var _teams = <Team>[];

  var _venues = <VenueSummary>[];

  String? _bookingId;

  String? _teamId;

  String? _venueId;

  final _city = TextEditingController();

  /// When, picked as a Nepal date and time.
  DateTime? _when;

  final _needed = TextEditingController(text: '2');

  final _cost = TextEditingController();

  SkillLevel? _skill;

  final _note = TextEditingController();

  var _busy = false;

  String? _error;

  Api get _api => ref.read(apiProvider);

  @override
  void initState() {
    super.initState();
    Future.wait([_api.bookings('upcoming'), _api.myTeams()]).then((results) {
      final bookings = results[0] as List<Booking>;
      final teams = results[1] as List<Team>;
      final live = bookings
          .where((b) => b.status == BookingStatus.confirmed || b.status == BookingStatus.pendingPayment)
          .toList();
      final led = teams.where((team) => team.leads).toList();
      if (mounted) {
        setState(() {
          _bookings = live;
          _bookingId = live.firstOrNull?.id;
          _teams = led;
          _teamId = led.firstOrNull?.id;
        });
      }
    }, onError: (Object error) {
      if (mounted) {
        setState(() => _error = problemOf(error).message);
      }
    });
    _api.venues(size: 50).then((page) {
      if (mounted) {
        setState(() => _venues = page.content);
      }
    }, onError: (_) {});
  }

  @override
  void dispose() {
    for (final field in [_city, _needed, _cost, _note]) {
      field.dispose();
    }
    super.dispose();
  }

  Future<void> _pickWhen() async {
    final now = DateTime.now();
    final day = await showDatePicker(
      context: context,
      initialDate: _when ?? now,
      firstDate: DateTime(now.year, now.month, now.day),
      lastDate: now.add(const Duration(days: 60)),
    );
    if (day == null || !mounted) {
      return;
    }
    final time = await showTimePicker(
      context: context,
      initialTime: _when != null ? TimeOfDay.fromDateTime(_when!) : const TimeOfDay(hour: 18, minute: 0),
    );
    if (time != null && mounted) {
      setState(() => _when = DateTime(day.year, day.month, day.day, time.hour, time.minute));
    }
  }

  /// "2026-10-20T18:00:00+05:45".
  static String _startsAt(DateTime when) {
    String two(int n) => n.toString().padLeft(2, '0');
    return '${when.year}-${two(when.month)}-${two(when.day)}T${two(when.hour)}:${two(when.minute)}:00$_nepalOffset';
  }

  bool get _canPost => _type == MatchPostType.needPlayers || _teams.isNotEmpty;

  Future<void> _post() async {
    final bookingId = _bookingId;
    final venueId = bookingId == null ? _venueId : null;
    final city = _city.text.trim();
    final when = _when;
    if (bookingId == null && (when == null || (venueId == null && city.isEmpty))) {
      setState(() => _error = l.sayWhereWhen);
      return;
    }
    final needPlayers = _type == MatchPostType.needPlayers;
    final cost = double.tryParse(_cost.text);
    FocusScope.of(context).unfocus();
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      final post = await _api.createMatchPost(
        type: _type,
        teamId: needPlayers ? null : _teamId,
        bookingId: bookingId,
        venueId: venueId,
        city: bookingId == null && venueId == null ? city : null,
        startsAt: bookingId == null ? _startsAt(when!) : null,
        playersNeeded: needPlayers ? int.tryParse(_needed.text) ?? 1 : null,
        costPerPlayer: needPlayers && cost != null && cost > 0 ? cost : null,
        skillLevel: _skill,
        note: _note.text.trim().isEmpty ? null : _note.text.trim(),
      );
      if (mounted) {
        context.pushReplacement('/matches/${post.id}');
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
    final c = context.fs;
    final text = Theme.of(context).textTheme;
    final when = _when;
    return Scaffold(
      appBar: AppBar(title: Text(l.postAGame)),
      body: ScreenBody(
        children: [
          Text(l.whatDoYouNeed, style: text.titleMedium),
          const SizedBox(height: 8),
          // Side by side at the same height, like the web's grid
          IntrinsicHeight(
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                for (final (i, (type, label, detail)) in [
                  (MatchPostType.needPlayers, l.needPlayersOption, l.needPlayersDetail),
                  (MatchPostType.findOpponent, l.needOpponentOption, l.needOpponentDetail),
                ].indexed) ...[
                  if (i > 0) const SizedBox(width: 8),
                  Expanded(
                    child: ChoiceTile(
                      selected: _type == type,
                      onTap: () => setState(() => _type = type),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(label, style: const TextStyle(fontWeight: FontWeight.w500)),
                          Text(detail, style: const TextStyle(fontSize: 14)),
                        ],
                      ),
                    ),
                  ),
                ],
              ],
            ),
          ),
          if (_type == MatchPostType.findOpponent) ...[
            const SizedBox(height: 12),
            if (_teams.isEmpty)
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                decoration: BoxDecoration(color: c.held.bg, borderRadius: BorderRadius.circular(8)),
                child: Row(
                  children: [
                    Expanded(
                      child: Text(
                        l.needCaptainForOpponent,
                        style: TextStyle(color: c.held.fg),
                      ),
                    ),
                    TextButton(onPressed: () => context.go('/teams'), child: const Text('Start a team')),
                  ],
                ),
              )
            else ...[
              FieldLabel(l.yourTeam),
              DropdownButtonFormField<String>(
                initialValue: _teamId,
                items: [for (final team in _teams) DropdownMenuItem(value: team.id, child: Text(team.name))],
                onChanged: (id) => setState(() => _teamId = id),
              ),
            ],
          ],
          const SizedBox(height: 20),
          Text(l.whereAndWhen, style: text.titleMedium),
          const SizedBox(height: 8),
          if (_bookings.isNotEmpty) ...[
            for (final b in _bookings)
              ChoiceTile(
                selected: _bookingId == b.id,
                onTap: () => setState(() => _bookingId = b.id),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(b.venue.name, style: const TextStyle(fontWeight: FontWeight.w500)),
                    Text(
                      '${dayLabel(b.startsAt.substring(0, 10))} · ${localClock(b.startsAt)} · ${b.court.name}',
                      style: const TextStyle(fontSize: 14, fontFeatures: tabular),
                    ),
                  ],
                ),
              ),
            ChoiceTile(
              selected: _bookingId == null,
              onTap: () => setState(() => _bookingId = null),
              child: Text(l.notBookedYet, style: const TextStyle(fontWeight: FontWeight.w500)),
            ),
          ],
          if (_bookingId == null) ...[
            const SizedBox(height: 4),
            FieldLabel(l.venue, optional: true),
            DropdownButtonFormField<String?>(
              initialValue: _venueId,
              isExpanded: true,
              items: [
                DropdownMenuItem(value: null, child: Text(l.notDecidedCity)),
                for (final v in _venues)
                  DropdownMenuItem(value: v.id, child: Text('${v.name}, ${v.city}', overflow: TextOverflow.ellipsis)),
              ],
              onChanged: (id) => setState(() => _venueId = id),
            ),
            if (_venueId == null) ...[
              const SizedBox(height: 12),
              FieldLabel(l.city),
              TextField(
                controller: _city,
                maxLength: 60,
                textCapitalization: TextCapitalization.words,
                decoration: InputDecoration(hintText: l.cityHint, counterText: ''),
              ),
            ],
            const SizedBox(height: 12),
            FieldLabel(l.when),
            OutlinedButton.icon(
              onPressed: _pickWhen,
              style: OutlinedButton.styleFrom(alignment: Alignment.centerLeft, minimumSize: const Size.fromHeight(48)),
              icon: const Icon(LucideIcons.calendarDays, size: 18),
              label: Text(
                when == null
                    ? l.pickDayTime
                    : '${dayLabel(_startsAt(when).substring(0, 10))} · ${clock(when.hour * 60 + when.minute)}',
                style: const TextStyle(fontFeatures: tabular),
              ),
            ),
          ],
          if (_type == MatchPostType.needPlayers) ...[
            const SizedBox(height: 20),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      FieldLabel(l.playersNeeded),
                      TextField(
                        controller: _needed,
                        keyboardType: TextInputType.number,
                        inputFormatters: [FilteringTextInputFormatter.digitsOnly, LengthLimitingTextInputFormatter(2)],
                        style: const TextStyle(fontFeatures: tabular),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      FieldLabel(l.eachPays, optional: true),
                      TextField(
                        controller: _cost,
                        keyboardType: const TextInputType.numberWithOptions(decimal: true),
                        inputFormatters: [FilteringTextInputFormatter.allow(RegExp(r'[0-9.]'))],
                        style: const TextStyle(fontFeatures: tabular),
                        decoration: InputDecoration(prefixText: inNepali ? 'रु ' : 'Rs '),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ],
          const SizedBox(height: 20),
          PillChoices(
            label: l.level,
            optional: true,
            options: [for (final s in SkillLevel.values) (s, s.label)],
            value: _skill,
            onChanged: (s) => setState(() => _skill = s),
          ),
          FieldLabel(l.note, optional: true),
          TextField(
            controller: _note,
            maxLength: 500,
            minLines: 2,
            maxLines: 4,
            textCapitalization: TextCapitalization.sentences,
            decoration: InputDecoration(hintText: l.postNoteHint, counterText: ''),
          ),
          if (_error case final error?) ...[const SizedBox(height: 16), Notice(error)],
          const SizedBox(height: 20),
          FilledButton(
            onPressed: _busy || !_canPost ? null : _post,
            style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(48)),
            child: BusyLabel(busy: _busy, label: l.postIt, busyLabel: l.posting),
          ),
        ],
      ),
    );
  }
}
