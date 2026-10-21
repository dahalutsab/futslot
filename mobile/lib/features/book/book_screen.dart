import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/api.dart';
import '../../core/api_error.dart';
import '../../core/api_types.dart';
import '../../core/theme.dart';
import '../../l10n/l10n.dart';
import '../../shared/booking_model.dart';
import '../../shared/format.dart';
import '../../shared/labels.dart';
import '../../shared/widgets.dart';

/// Booking one slot: extras, a membership or discount code, what it costs and what to pay now, then confirm.
class BookScreen extends ConsumerStatefulWidget {
  const BookScreen({super.key, required this.slug, required this.courtId, required this.startsAt});

  final String slug;
  final String courtId;
  final String startsAt;

  @override
  ConsumerState<BookScreen> createState() => _BookScreenState();
}

class _BookScreenState extends ConsumerState<BookScreen> {
  final _code = TextEditingController();

  final _notes = TextEditingController();

  VenueDetail? _venue;

  var _extras = <AddOnItem>[];

  /// Quantity by add-on id.
  var _picked = <String, int>{};

  /// The code sent with the quote; set by Apply.
  String? _appliedCode;

  /// The player's active memberships at this venue.
  var _memberships = <Membership>[];

  /// The membership used for this booking, instead of a discount code.
  String? _membershipId;

  Quote? _quote;

  /// Why this slot can't be booked as asked (taken, too far ahead...).
  String? _problem;

  String? _codeError;

  String? _error;

  var _busy = false;

  /// Only the latest quote counts when the player changes things quickly.
  var _quoteRun = 0;

  Api get _api => ref.read(apiProvider);

  bool get _depositBlocks => (_quote?.depositToAdd ?? 0) > 0;

  BookingRequest get _request => BookingRequest(
        courtId: widget.courtId,
        startsAt: widget.startsAt,
        membershipId: _membershipId,
        discountCode: _membershipId == null ? _appliedCode : null,
        addOns: _picked,
      );

  @override
  void initState() {
    super.initState();
    _load();
    _price();
  }

  @override
  void dispose() {
    _code.dispose();
    _notes.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    try {
      final results = await Future.wait([_api.venue(widget.slug), _api.addOns(widget.slug), _api.memberships()]);
      final venue = results[0] as VenueDetail;
      final extras = results[1] as List<AddOnItem>;
      final memberships = results[2] as List<Membership>;
      if (!mounted) {
        return;
      }
      setState(() {
        _venue = venue;
        _extras = extras.where((item) => item.active).toList();
        _memberships = memberships.where((m) => m.venue.slug == widget.slug && m.active).toList();
      });
    } catch (error) {
      if (mounted) {
        setState(() => _problem = problemOf(error).message);
      }
    }
  }

  Future<void> _price() async {
    final run = ++_quoteRun;
    final request = _request;
    try {
      final quote = await _api.quote(request);
      if (mounted && run == _quoteRun) {
        setState(() {
          _quote = quote;
          _problem = null;
        });
      }
    } catch (error) {
      if (!mounted || run != _quoteRun) {
        return;
      }
      final problem = problemOf(error);
      if (request.discountCode != null && RegExp('CODE|DISCOUNT').hasMatch(problem.code ?? '')) {
        // A bad code shouldn't hide the price: show why and quote without it
        setState(() {
          _codeError = problem.message;
          _appliedCode = null;
        });
        _price();
        return;
      }
      setState(() {
        _quote = null;
        _problem = problem.message;
      });
    }
  }

  void _change(AddOnItem item, int by) {
    setState(() => _picked = {..._picked, item.id: ((_picked[item.id] ?? 0) + by).clamp(0, 50)});
    _price();
  }

  void _useMembership(String? id) {
    setState(() => _membershipId = id);
    _price();
  }

  void _applyCode() {
    FocusScope.of(context).unfocus();
    final code = _code.text.trim().toUpperCase();
    setState(() {
      _codeError = null;
      _appliedCode = code.isEmpty ? null : code;
    });
    _price();
  }

  void _removeCode() {
    _code.clear();
    setState(() {
      _appliedCode = null;
      _codeError = null;
    });
    _price();
  }

  Future<void> _confirm() async {
    if (_busy || _quote == null || _depositBlocks) {
      return;
    }
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      final notes = _notes.text.trim();
      final booking = await _api.book(_request.withNotes(notes.isEmpty ? null : notes));
      if (mounted) {
        context.go('/bookings/${booking.id}?booked=1');
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
    final venue = _venue;
    final courtName = venue?.courts.where((court) => court.id == widget.courtId).firstOrNull?.name;
    final when = widget.startsAt.length >= 16
        ? '${dayLabel(widget.startsAt.substring(0, 10))} · ${localClock(widget.startsAt)}'
        : '';
    return Scaffold(
      appBar: AppBar(title: Text(venue?.name ?? '')),
      body: ScreenBody(
        children: [
          Text(l.bookThisSlot, style: text.headlineSmall),
          const SizedBox(height: 4),
          Text(
            '$when${courtName != null ? ' · $courtName' : ''}',
            style: TextStyle(color: c.muted, fontFeatures: tabular),
          ),
          const SizedBox(height: 24),
          if (_problem case final problem?)
            FsCard(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(l.slotUnavailable, style: const TextStyle(fontWeight: FontWeight.w500)),
                  const SizedBox(height: 4),
                  Text(problem, style: TextStyle(color: c.muted)),
                  const SizedBox(height: 16),
                  FilledButton(
                    onPressed: () => context.canPop() ? context.pop() : context.go('/venues/${widget.slug}'),
                    child: Text(l.seeOtherSlots),
                  ),
                ],
              ),
            )
          else
            ..._form(context),
        ],
      ),
    );
  }

  List<Widget> _form(BuildContext context) {
    final c = context.fs;
    final text = Theme.of(context).textTheme;
    final quote = _quote;
    return [
      if (_depositBlocks && quote != null) ...[
        FsCard(
          color: c.held.bg,
          borderColor: c.held.line,
          child: DefaultTextStyle.merge(
            style: TextStyle(color: c.held.fg),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(l.addDepositFirst, style: const TextStyle(fontWeight: FontWeight.w600)),
                const SizedBox(height: 4),
                Text(
                  l.depositExplain(_venue?.name ?? l.thisVenue, rupees(depositAmount)),
                  style: const TextStyle(fontSize: 14),
                ),
                const SizedBox(height: 12),
                FilledButton(
                  onPressed: () async {
                    final back = Uri(
                      path: '/venues/${widget.slug}/book',
                      queryParameters: {'court': widget.courtId, 'start': widget.startsAt},
                    ).toString();
                    await context.push(Uri(path: '/me/deposit', queryParameters: {'back': back}).toString());
                    _price();
                  },
                  child: Text(l.addAmount(rupees(quote.depositToAdd))),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 16),
      ],
      if (_extras.isNotEmpty) ...[
        FsCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(l.extras, style: text.titleMedium),
              Text(l.extrasPaidAtVenue, style: TextStyle(fontSize: 14, color: c.muted)),
              const SizedBox(height: 8),
              for (final (index, item) in _extras.indexed) ...[
                if (index > 0) const Divider(),
                _ExtraRow(item: item, quantity: _picked[item.id] ?? 0, onChange: (by) => _change(item, by)),
              ],
            ],
          ),
        ),
        const SizedBox(height: 16),
      ],
      if (_memberships.isNotEmpty) ...[
        FsCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(l.useMembership, style: text.titleMedium),
              const SizedBox(height: 8),
              ChoiceTile(
                selected: _membershipId == null,
                onTap: () => _useMembership(null),
                child: Text(l.dontUseOne),
              ),
              for (final membership in _memberships)
                ChoiceTile(
                  selected: _membershipId == membership.id,
                  onTap: () => _useMembership(membership.id),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(membership.planName, style: const TextStyle(fontWeight: FontWeight.w500)),
                      Text(
                        '${planTerms(
                          membership.planType,
                          gamesIncluded: membership.gamesIncluded,
                          discountPercent: membership.discountPercent,
                        )}'
                        '${membership.gamesRemaining != null ? ' · ${l.gamesLeft(membership.gamesRemaining!)}' : ''}',
                        style: TextStyle(fontSize: 14, color: c.muted),
                      ),
                    ],
                  ),
                ),
            ],
          ),
        ),
        const SizedBox(height: 16),
      ],
      if (_membershipId == null) ...[
        FsCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(l.discountCode, style: text.titleMedium),
              const SizedBox(height: 8),
              if (_appliedCode != null && quote?.discountCode != null)
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                      decoration: BoxDecoration(color: c.confirmed.bg, borderRadius: BorderRadius.circular(6)),
                      child: Text(
                        quote!.discountCode!,
                        style: TextStyle(fontWeight: FontWeight.w500, color: c.confirmed.fg, fontFeatures: tabular),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Text('−${rupees(quote.discount)}', style: TextStyle(color: c.muted, fontFeatures: tabular)),
                    const Spacer(),
                    TextButton(onPressed: _removeCode, child: Text(l.remove)),
                  ],
                )
              else ...[
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: _code,
                        maxLength: 32,
                        autocorrect: false,
                        textCapitalization: TextCapitalization.characters,
                        textInputAction: TextInputAction.done,
                        onSubmitted: (_) => _applyCode(),
                        decoration: InputDecoration(hintText: l.codeHint, counterText: ''),
                      ),
                    ),
                    const SizedBox(width: 8),
                    OutlinedButton(onPressed: _applyCode, child: Text(l.apply)),
                  ],
                ),
                if (_codeError case final error?) ...[const SizedBox(height: 8), Notice(error)],
              ],
            ],
          ),
        ),
        const SizedBox(height: 16),
      ],
      FsCard(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            FieldLabel(l.noteForVenue, optional: true),
            TextField(
              controller: _notes,
              maxLength: 500,
              minLines: 2,
              maxLines: 4,
              textCapitalization: TextCapitalization.sentences,
              decoration: InputDecoration(hintText: l.noteHint, counterText: ''),
            ),
          ],
        ),
      ),
      const SizedBox(height: 16),
      if (quote == null)
        Skeleton(height: 160, label: l.workingOutPrice)
      else ...[
        FsCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              AmountRow(label: l.court, value: rupees(quote.price)),
              if (quote.addOns > 0) AmountRow(label: l.extras, value: rupees(quote.addOns)),
              if (quote.discount > 0)
                AmountRow(label: l.discount, value: '−${rupees(quote.discount)}', valueColor: c.confirmed.fg),
              const Padding(padding: EdgeInsets.symmetric(vertical: 6), child: Divider()),
              AmountRow(label: l.total, value: rupees(quote.total), strong: true),
              if (quote.advanceRequired > 0) ...[
                AmountRow(label: l.payNowToConfirm, value: rupees(quote.advanceRequired), strong: true),
                AmountRow(label: l.atTheVenue, value: rupees(quote.total - quote.advanceRequired)),
              ] else
                AmountRow(label: l.pay, value: l.atTheVenue),
              if (quote.strikeAdvance) ...[
                const SizedBox(height: 12),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  decoration: BoxDecoration(color: c.noshow.bg, borderRadius: BorderRadius.circular(8)),
                  child: Text(
                    l.strikeAdvanceNote,
                    style: TextStyle(fontSize: 14, color: c.noshow.fg),
                  ),
                ),
              ],
            ],
          ),
        ),
        if (_venue case final venue?) ...[
          const SizedBox(height: 16),
          FsCard(
            padding: EdgeInsets.zero,
            child: Theme(
              data: Theme.of(context).copyWith(dividerColor: Colors.transparent),
              child: ExpansionTile(
                title: Text(l.bookingRules, style: text.titleMedium),
                childrenPadding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
                expandedCrossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  for (final line in policyLines(venue.policy))
                    Padding(
                      padding: const EdgeInsets.only(bottom: 4),
                      child: Text('•  $line', style: TextStyle(fontSize: 14, color: c.muted)),
                    ),
                ],
              ),
            ),
          ),
        ],
        if (_error case final error?) ...[const SizedBox(height: 16), Notice(error)],
        if (!_depositBlocks) ...[
          const SizedBox(height: 20),
          FilledButton(
            onPressed: _busy ? null : _confirm,
            child: BusyLabel(
              busy: _busy,
              label: quote.advanceRequired > 0 ? l.bookAndPay(rupees(quote.advanceRequired)) : l.book,
              busyLabel: l.booking_,
              icon: LucideIcons.shieldCheck,
            ),
          ),
          if (quote.advanceRequired > 0 && _venue != null) ...[
            const SizedBox(height: 8),
            Text(
              l.holdWhilePay(_venue!.policy.bookingHoldMinutes),
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 14, color: c.subtle),
            ),
          ],
        ],
      ],
    ];
  }
}

class _ExtraRow extends StatelessWidget {
  const _ExtraRow({required this.item, required this.quantity, required this.onChange});

  final AddOnItem item;
  final int quantity;
  final ValueChanged<int> onChange;

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(item.name),
                Text(
                  l.priceEach(rupees(item.price)),
                  style: TextStyle(fontSize: 14, color: c.muted, fontFeatures: tabular),
                ),
              ],
            ),
          ),
          _StepButton(
            icon: LucideIcons.minus,
            tooltip: l.oneLess(item.name),
            onTap: quantity > 0 ? () => onChange(-1) : null,
          ),
          SizedBox(
            width: 32,
            child: Semantics(
              liveRegion: true,
              child: Text(
                digits(quantity),
                textAlign: TextAlign.center,
                style: const TextStyle(fontWeight: FontWeight.w500, fontFeatures: tabular),
              ),
            ),
          ),
          _StepButton(icon: LucideIcons.plus, tooltip: l.oneMore(item.name), onTap: () => onChange(1)),
        ],
      ),
    );
  }
}

class _StepButton extends StatelessWidget {
  const _StepButton({required this.icon, required this.tooltip, required this.onTap});

  final IconData icon;
  final String tooltip;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return SizedBox.square(
      dimension: 44,
      child: OutlinedButton(
        onPressed: onTap,
        style: OutlinedButton.styleFrom(padding: EdgeInsets.zero, minimumSize: const Size.square(44)),
        child: Icon(icon, size: 16, semanticLabel: tooltip),
      ),
    );
  }
}
