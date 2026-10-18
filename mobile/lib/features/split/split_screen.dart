import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/api.dart';
import '../../core/api_error.dart';
import '../../core/api_types.dart';
import '../../core/theme.dart';
import '../../shared/format.dart';
import '../../shared/labels.dart';
import '../../shared/widgets.dart';

/// Sharing a booking's cost among the players: equally, or baazi-style where the losers pay more. The booker sets
/// the rule, adds players to two sides, records the result and ticks off who has paid them; the other players see
/// what they owe and to whom.
class SplitScreen extends ConsumerStatefulWidget {
  const SplitScreen({super.key, required this.bookingId});

  final String bookingId;

  @override
  ConsumerState<SplitScreen> createState() => _SplitScreenState();
}

class _SplitScreenState extends ConsumerState<SplitScreen> {
  CostSplit? _split;

  /// The booker hasn't set one up yet.
  var _none = false;

  String? _error;

  var _busy = false;

  String? _actionError;

  var _editing = false;

  // The rules form
  var _rule = SplitRule.equal;
  final _percent = TextEditingController(text: '70');
  final _extras = TextEditingController(text: '0');
  final _extrasNote = TextEditingController();
  final _sideA = TextEditingController();
  final _sideB = TextEditingController();
  var _mySide = Side.a;

  // Adding a player
  final _newName = TextEditingController();
  final _newPhone = TextEditingController();
  var _newSide = Side.a;

  Api get _api => ref.read(apiProvider);

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    for (final field in [_percent, _extras, _extrasNote, _sideA, _sideB, _newName, _newPhone]) {
      field.dispose();
    }
    super.dispose();
  }

  Future<void> _load() async {
    try {
      final split = await _api.split(widget.bookingId);
      if (mounted) {
        setState(() {
          _split = split;
          _none = false;
          _error = null;
        });
      }
    } catch (error) {
      final problem = problemOf(error);
      if (mounted) {
        setState(() => problem.code == 'SPLIT_NOT_FOUND' ? _none = true : _error = problem.message);
      }
    }
  }

  void _edit(CostSplit split) {
    setState(() {
      _rule = split.rule;
      _percent.text = '${split.losersPercent ?? 70}';
      _extras.text = _plain(split.extrasAmount);
      _extrasNote.text = split.extrasNote ?? '';
      _sideA.text = split.sideAName;
      _sideB.text = split.sideBName;
      _actionError = null;
      _editing = true;
    });
  }

  static String _plain(double value) => value == value.roundToDouble() ? value.toInt().toString() : '$value';

  static String? _text(TextEditingController field) => field.text.trim().isEmpty ? null : field.text.trim();

  Future<void> _saveRules() => _act(() async {
        final split = await _api.setUpSplit(
          widget.bookingId,
          rule: _rule,
          losersPercent: _rule == SplitRule.losersPayPercent ? int.tryParse(_percent.text) ?? 70 : null,
          extrasAmount: double.tryParse(_extras.text) ?? 0,
          extrasNote: _text(_extrasNote),
          sideAName: _text(_sideA),
          sideBName: _text(_sideB),
          bookerSide: _split == null ? _mySide : null,
        );
        setState(() {
          _split = split;
          _none = false;
          _editing = false;
        });
      });

  Future<void> _addPlayer() {
    final name = _text(_newName);
    final phoneNumber = _newPhone.text.replaceAll(RegExp(r'\D'), '');
    if (name == null && phoneNumber.isEmpty) {
      setState(() => _actionError = 'Give their name, or the phone number they use FutSlot with.');
      return Future.value();
    }
    return _act(() async {
      final split = await _api.addSplitPlayer(
        widget.bookingId,
        name: name,
        phone: phoneNumber.isEmpty ? null : phoneNumber,
        side: _newSide,
      );
      setState(() => _split = split);
      _newName.clear();
      _newPhone.clear();
    });
  }

  /// Runs [action], showing what went wrong if it fails. One at a time.
  Future<void> _act(Future<void> Function() action) async {
    if (_busy) {
      return;
    }
    setState(() {
      _busy = true;
      _actionError = null;
    });
    FocusScope.of(context).unfocus();
    try {
      await action();
    } catch (error) {
      if (mounted) {
        setState(() => _actionError = problemOf(error).message);
      }
    } finally {
      if (mounted) {
        setState(() => _busy = false);
      }
    }
  }

  Future<void> _update(Future<CostSplit> Function() call) => _act(() async {
        final split = await call();
        setState(() => _split = split);
      });

  @override
  Widget build(BuildContext context) {
    final split = _split;
    return Scaffold(
      appBar: AppBar(title: const Text('Split the cost')),
      body: switch ((split, _error)) {
        (_, final String error) => ScreenBody(children: [ErrorPanel(message: error, onRetry: _load)]),
        (final CostSplit s, _) when !_editing => ScreenBody(onRefresh: _load, children: _view(context, s)),
        (final CostSplit s, _) => ScreenBody(children: _rulesForm(context, s)),
        _ when _none => ScreenBody(children: _rulesForm(context, null)),
        _ => const ScreenBody(children: [Skeleton(height: 192)]),
      },
    );
  }

  List<Widget> _rulesForm(BuildContext context, CostSplit? split) {
    final c = context.fs;
    final text = Theme.of(context).textTheme;
    final digits = [FilteringTextInputFormatter.digitsOnly];
    return [
      Text(
        split != null
            ? 'Change how the cost is shared.'
            : 'Share what this game costs with the players: equally, or baazi-style.',
        style: TextStyle(color: c.muted),
      ),
      const SizedBox(height: 20),
      Text('Who pays', style: text.titleMedium),
      const SizedBox(height: 8),
      for (final rule in SplitRule.values)
        ChoiceTile(
          selected: _rule == rule,
          onTap: () => setState(() => _rule = rule),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(rule.label, style: const TextStyle(fontWeight: FontWeight.w500)),
              Text(rule.detail, style: const TextStyle(fontSize: 14)),
            ],
          ),
        ),
      if (_rule == SplitRule.losersPayPercent) ...[
        const SizedBox(height: 12),
        const FieldLabel('The losing side pays'),
        Row(
          children: [
            SizedBox(
              width: 96,
              child: TextField(
                controller: _percent,
                keyboardType: TextInputType.number,
                inputFormatters: digits,
                style: const TextStyle(fontFeatures: tabular),
              ),
            ),
            const SizedBox(width: 8),
            Text('% of everything', style: TextStyle(color: c.muted)),
          ],
        ),
      ],
      const SizedBox(height: 20),
      const FieldLabel('One side'),
      TextField(
        controller: _sideA,
        maxLength: 30,
        decoration: const InputDecoration(hintText: 'Team A', counterText: ''),
      ),
      const SizedBox(height: 12),
      const FieldLabel('The other side'),
      TextField(
        controller: _sideB,
        maxLength: 30,
        decoration: const InputDecoration(hintText: 'Team B', counterText: ''),
      ),
      if (split == null) ...[
        const SizedBox(height: 12),
        const FieldLabel('You play for'),
        ListenableBuilder(
          listenable: Listenable.merge([_sideA, _sideB]),
          builder: (context, _) => Segmented(
            options: [(Side.a, _text(_sideA) ?? 'Team A'), (Side.b, _text(_sideB) ?? 'Team B')],
            value: _mySide,
            onChanged: (side) => setState(() => _mySide = side),
          ),
        ),
      ],
      const SizedBox(height: 20),
      Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 128,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const FieldLabel('Extras (Rs)'),
                TextField(
                  controller: _extras,
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  inputFormatters: [FilteringTextInputFormatter.allow(RegExp(r'[0-9.]'))],
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
                const FieldLabel('For', optional: true),
                TextField(
                  controller: _extrasNote,
                  maxLength: 100,
                  decoration: const InputDecoration(hintText: 'Water, drinks…', counterText: ''),
                ),
              ],
            ),
          ),
        ],
      ),
      if (_actionError case final error?) ...[const SizedBox(height: 16), Notice(error)],
      const SizedBox(height: 20),
      Row(
        children: [
          FilledButton(
            onPressed: _busy ? null : _saveRules,
            style: FilledButton.styleFrom(minimumSize: const Size(0, 48)),
            child: BusyLabel(busy: _busy, label: split != null ? 'Save' : 'Start splitting', busyLabel: 'Saving…'),
          ),
          if (split != null) ...[
            const SizedBox(width: 8),
            TextButton(onPressed: () => setState(() => _editing = false), child: const Text('Cancel')),
          ],
        ],
      ),
    ];
  }

  List<Widget> _view(BuildContext context, CostSplit s) {
    final c = context.fs;
    final text = Theme.of(context).textTheme;
    final started = !DateTime.parse(s.startsAt).isAfter(DateTime.now());
    final me = s.players.where((p) => p.you).firstOrNull;
    return [
      Text(
        '${dayLabel(s.startsAt.substring(0, 10))} · ${localClock(s.startsAt)} · ${s.venueName}, ${s.courtName}',
        style: TextStyle(color: c.muted, fontFeatures: tabular),
      ),
      const SizedBox(height: 16),
      FsCard(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        s.rule == SplitRule.losersPayPercent ? '${s.rule.label} (${s.losersPercent}%)' : s.rule.label,
                        style: const TextStyle(fontWeight: FontWeight.w600),
                      ),
                      Text(s.rule.detail, style: TextStyle(fontSize: 14, color: c.muted)),
                    ],
                  ),
                ),
                if (s.youAreBooker && s.result == null)
                  OutlinedButton(onPressed: () => _edit(s), child: const Text('Change')),
              ],
            ),
            const SizedBox(height: 12),
            AmountRow(label: 'Court', value: rupees(s.courtAmount)),
            if (s.extrasAmount > 0)
              AmountRow(
                label: s.extrasNote != null ? 'Extras · ${s.extrasNote}' : 'Extras',
                value: rupees(s.extrasAmount),
              ),
            AmountRow(label: 'Total', value: rupees(s.total), strong: true),
            if (s.result == null && s.rule != SplitRule.equal && s.outcomes.isNotEmpty) ...[
              const SizedBox(height: 16),
              _Outcomes(split: s),
            ],
          ],
        ),
      ),
      if (!s.youAreBooker && me != null) ...[const SizedBox(height: 16), _YourShare(split: s, me: me)],
      if (s.result case final result?) ...[
        const SizedBox(height: 16),
        FsCard(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      s.rule == SplitRule.equal ? 'Played' : resultLabel(result, s.sideAName, s.sideBName),
                      style: const TextStyle(fontWeight: FontWeight.w600),
                    ),
                  ),
                  if (s.youAreBooker)
                    TextButton(
                      onPressed: _busy ? null : () => _update(() => _api.undoSplitResult(s.bookingId)),
                      style: TextButton.styleFrom(foregroundColor: c.muted),
                      child: const Text('Undo'),
                    ),
                ],
              ),
              if (s.youAreBooker)
                Text(
                  '${rupees(s.collected)} collected · ${rupees(s.outstanding)} still to come',
                  style: TextStyle(fontSize: 14, color: c.muted, fontFeatures: tabular),
                ),
            ],
          ),
        ),
      ] else if (s.youAreBooker && started) ...[
        const SizedBox(height: 16),
        FsCard(
          padding: const EdgeInsets.all(20),
          borderColor: c.held.line,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                s.rule == SplitRule.equal ? 'Played the game?' : 'Who won?',
                style: const TextStyle(fontWeight: FontWeight.w600),
              ),
              Text(
                "This works out everyone's share and tells the players with the app.",
                style: TextStyle(fontSize: 14, color: c.muted),
              ),
              const SizedBox(height: 12),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  if (s.rule == SplitRule.equal)
                    FilledButton(
                      onPressed: _busy ? null : () => _update(() => _api.splitResult(s.bookingId, null)),
                      child: const Text('Work out shares'),
                    )
                  else ...[
                    for (final (result, label) in [(MatchResult.aWon, s.sideAName), (MatchResult.bWon, s.sideBName)])
                      FilledButton(
                        onPressed: _busy ? null : () => _update(() => _api.splitResult(s.bookingId, result)),
                        child: Text(label),
                      ),
                    OutlinedButton(
                      onPressed: _busy ? null : () => _update(() => _api.splitResult(s.bookingId, MatchResult.draw)),
                      child: const Text('Draw'),
                    ),
                  ],
                ],
              ),
            ],
          ),
        ),
      ],
      if (_actionError case final error?) ...[const SizedBox(height: 16), Notice(error)],
      for (final side in Side.values) ...[
        const SizedBox(height: 16),
        _SideCard(
          split: s,
          side: side,
          busy: _busy,
          onPaid: (p) => _update(() => _api.splitPaid(s.bookingId, p.id, !p.settled)),
          onMove: (p) => _update(() => _api.moveSplitPlayer(s.bookingId, p.id, p.side == Side.a ? Side.b : Side.a)),
          onRemove: (p) => _update(() => _api.removeSplitPlayer(s.bookingId, p.id)),
        ),
      ],
      if (s.youAreBooker && s.result == null) ...[
        const SizedBox(height: 16),
        FsCard(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text('Add a player', style: text.titleMedium),
              Text(
                'By the phone number they use FutSlot with, so they see their share, or just a name.',
                style: TextStyle(fontSize: 14, color: c.muted),
              ),
              const SizedBox(height: 12),
              const FieldLabel('Phone'),
              TextField(
                controller: _newPhone,
                keyboardType: TextInputType.phone,
                autofillHints: const [AutofillHints.telephoneNumber],
                style: const TextStyle(fontFeatures: tabular),
                decoration: const InputDecoration(hintText: '98XXXXXXXX'),
              ),
              const SizedBox(height: 12),
              const FieldLabel('Name'),
              TextField(
                controller: _newName,
                maxLength: 60,
                textCapitalization: TextCapitalization.words,
                decoration: const InputDecoration(counterText: ''),
              ),
              const SizedBox(height: 12),
              Segmented(
                options: [(Side.a, s.sideAName), (Side.b, s.sideBName)],
                value: _newSide,
                onChanged: (side) => setState(() => _newSide = side),
              ),
              const SizedBox(height: 12),
              Align(
                alignment: Alignment.centerLeft,
                child: FilledButton(onPressed: _busy ? null : _addPlayer, child: const Text('Add')),
              ),
            ],
          ),
        ),
      ],
    ];
  }
}

/// "Each player pays, roughly": per side, for each result.
class _Outcomes extends StatelessWidget {
  const _Outcomes({required this.split});

  final CostSplit split;

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    TableRow row(List<String> cells, {bool head = false}) => TableRow(
          decoration: head ? null : BoxDecoration(border: Border(top: BorderSide(color: c.border))),
          children: [
            for (final (i, cell) in cells.indexed)
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 6),
                child: Text(
                  cell,
                  textAlign: i == 0 ? TextAlign.left : TextAlign.right,
                  style: TextStyle(fontSize: 14, color: head ? c.muted : null, fontFeatures: tabular),
                ),
              ),
          ],
        );
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text('Each player pays, roughly', style: TextStyle(fontSize: 14, fontWeight: FontWeight.w500)),
        const SizedBox(height: 4),
        Table(
          columnWidths: const {0: FlexColumnWidth(1.4)},
          children: [
            row(['If', split.sideAName, split.sideBName], head: true),
            for (final o in split.outcomes)
              row([resultLabel(o.result, split.sideAName, split.sideBName), rupees(o.sideAEach), rupees(o.sideBEach)]),
          ],
        ),
      ],
    );
  }
}

/// What a player other than the booker owes, and how to pay it.
class _YourShare extends StatelessWidget {
  const _YourShare({required this.split, required this.me});

  final CostSplit split;
  final SplitPlayer me;

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    final booker = split.bookedBy;
    final share = me.share;
    return FsCard(
      padding: const EdgeInsets.all(20),
      color: me.settled ? c.confirmed.bg : null,
      borderColor: me.settled ? c.confirmed.line : null,
      child: switch (share) {
        null => Text('Your share is worked out once ${booker.name} records the result.'),
        _ when me.settled => Text(
            'Paid. ${booker.name} has your ${rupees(share)}.',
            style: TextStyle(fontWeight: FontWeight.w500, color: c.confirmed.fg),
          ),
        _ => Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('You owe ${booker.name} ${rupees(share)}', style: const TextStyle(fontWeight: FontWeight.w600)),
              if (booker.phone case final number?) ...[
                const SizedBox(height: 4),
                Text(
                  "Pay them on eSewa or Khalti: ${phone(number)}. They tick you off once it's in.",
                  style: TextStyle(color: c.muted, fontFeatures: tabular),
                ),
              ],
            ],
          ),
      },
    );
  }
}

/// One side's players. Before the result the booker moves and removes players; after it, ticks off who paid.
class _SideCard extends StatelessWidget {
  const _SideCard({
    required this.split,
    required this.side,
    required this.busy,
    required this.onPaid,
    required this.onMove,
    required this.onRemove,
  });

  final CostSplit split;
  final Side side;
  final bool busy;
  final ValueChanged<SplitPlayer> onPaid;
  final ValueChanged<SplitPlayer> onMove;
  final ValueChanged<SplitPlayer> onRemove;

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    final players = split.players.where((p) => p.side == side).toList();
    final name = split.sideName(side);
    return FsCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Semantics(
            header: true,
            child: Text.rich(
              TextSpan(
                text: name,
                style: const TextStyle(fontWeight: FontWeight.w600),
                children: [
                  TextSpan(text: ' · ${players.length}', style: TextStyle(fontWeight: FontWeight.w400, color: c.muted)),
                ],
              ),
            ),
          ),
          const SizedBox(height: 4),
          for (final (i, p) in players.indexed) ...[
            if (i > 0) const Divider(),
            ConstrainedBox(
              constraints: const BoxConstraints(minHeight: 48),
              child: Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text.rich(
                          TextSpan(
                            text: p.name,
                            children: [
                              if (p.you)
                                TextSpan(text: ' (you)', style: TextStyle(color: c.muted))
                              else if (p.booker)
                                TextSpan(text: ' (booked)', style: TextStyle(color: c.muted)),
                            ],
                          ),
                          overflow: TextOverflow.ellipsis,
                        ),
                        if (p.phone != null && split.youAreBooker)
                          Text(phone(p.phone), style: TextStyle(fontSize: 12, color: c.subtle, fontFeatures: tabular)),
                      ],
                    ),
                  ),
                  if (p.share case final share?)
                    Text(
                      rupees(share),
                      style: TextStyle(
                        fontSize: 14,
                        fontFeatures: tabular,
                        color: p.settled ? c.subtle : null,
                        decoration: p.settled && !p.booker ? TextDecoration.lineThrough : null,
                      ),
                    ),
                  if (split.youAreBooker && !p.booker)
                    if (split.result != null)
                      Padding(
                        padding: const EdgeInsets.only(left: 8),
                        child: IconButton(
                          tooltip: p.settled ? 'Paid: ${p.name}' : 'Mark paid: ${p.name}',
                          isSelected: p.settled,
                          onPressed: busy ? null : () => onPaid(p),
                          style: IconButton.styleFrom(
                            backgroundColor: p.settled ? c.confirmed.bg : null,
                            side: BorderSide(color: p.settled ? c.confirmed.line : c.border),
                            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(6)),
                          ),
                          icon: Icon(LucideIcons.check, size: 16, color: p.settled ? c.confirmed.fg : c.subtle),
                        ),
                      )
                    else ...[
                      IconButton(
                        tooltip: 'Move ${p.name} to the other side',
                        onPressed: busy ? null : () => onMove(p),
                        icon: Icon(LucideIcons.arrowLeftRight, size: 16, color: c.subtle),
                      ),
                      IconButton(
                        tooltip: 'Remove ${p.name}',
                        onPressed: busy ? null : () => onRemove(p),
                        icon: Icon(LucideIcons.trash2, size: 16, color: c.subtle),
                      ),
                    ],
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }
}
