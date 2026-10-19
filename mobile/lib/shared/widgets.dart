import 'package:flutter/material.dart';

import '../core/theme.dart';
import '../l10n/l10n.dart';

/// A bordered surface, the app's basic block. [color] tints it for a state (held, being checked...).
class FsCard extends StatelessWidget {
  const FsCard({super.key, required this.child, this.padding = const EdgeInsets.all(16), this.color, this.borderColor});

  final Widget child;
  final EdgeInsetsGeometry padding;
  final Color? color;
  final Color? borderColor;

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    // A Material, so rows inside it show their tap ripple
    return Material(
      color: color ?? c.surface,
      clipBehavior: Clip.antiAlias,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(color: borderColor ?? c.border),
      ),
      child: Padding(padding: padding, child: SizedBox(width: double.infinity, child: child)),
    );
  }
}

/// Screen padding and the one-column width tasks are kept to, so a tablet doesn't stretch a form.
class ScreenBody extends StatelessWidget {
  const ScreenBody({super.key, required this.children, this.onRefresh, this.maxWidth = 576, this.controller});

  final List<Widget> children;
  final ScrollController? controller;
  final Future<void> Function()? onRefresh;
  final double maxWidth;

  @override
  Widget build(BuildContext context) {
    // Built all at once (screens are short), so a section further down can be scrolled to
    final list = SingleChildScrollView(
      controller: controller,
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
      child: Center(
        child: ConstrainedBox(
          constraints: BoxConstraints(maxWidth: maxWidth),
          child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: children),
        ),
      ),
    );
    return onRefresh == null ? list : RefreshIndicator(onRefresh: onRefresh!, child: list);
  }
}

/// A placeholder block while something loads.
class Skeleton extends StatefulWidget {
  const Skeleton({super.key, this.height = 96, this.label});

  final double height;

  /// Read out while it shows; "Loading" unless set.
  final String? label;

  @override
  State<Skeleton> createState() => _SkeletonState();
}

class _SkeletonState extends State<Skeleton> with SingleTickerProviderStateMixin {
  late final _pulse = AnimationController(vsync: this, duration: const Duration(milliseconds: 1000))
    ..repeat(reverse: true);

  @override
  void dispose() {
    _pulse.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: widget.label ?? l.loading,
      child: FadeTransition(
        opacity: Tween(begin: 0.5, end: 1.0).animate(_pulse),
        child: Container(
          height: widget.height,
          decoration: BoxDecoration(color: context.fs.surface, borderRadius: BorderRadius.circular(12)),
        ),
      ),
    );
  }
}

/// Something failed to load, with a way to try again.
class ErrorPanel extends StatelessWidget {
  const ErrorPanel({super.key, required this.message, this.onRetry});

  final String message;
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) {
    return FsCard(
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 40),
      child: Column(
        children: [
          Text(message, textAlign: TextAlign.center, style: TextStyle(color: context.fs.muted)),
          if (onRetry != null) ...[
            const SizedBox(height: 16),
            FilledButton(onPressed: onRetry, child: Text(l.tryAgain)),
          ],
        ],
      ),
    );
  }
}

/// Nothing here yet: what this place is for, and maybe where to start.
class EmptyPanel extends StatelessWidget {
  const EmptyPanel({super.key, required this.title, required this.body, this.action});

  final String title;
  final String body;
  final Widget? action;

  @override
  Widget build(BuildContext context) {
    return FsCard(
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 48),
      child: Column(
        children: [
          Text(title, textAlign: TextAlign.center, style: const TextStyle(fontWeight: FontWeight.w500)),
          const SizedBox(height: 4),
          Text(body, textAlign: TextAlign.center, style: TextStyle(color: context.fs.muted)),
          if (action != null) ...[const SizedBox(height: 16), action!],
        ],
      ),
    );
  }
}

/// A button's label, or a spinner and what's happening while it works.
class BusyLabel extends StatelessWidget {
  const BusyLabel({super.key, required this.busy, required this.label, required this.busyLabel, this.icon});

  final bool busy;
  final String label;
  final String busyLabel;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    final color = DefaultTextStyle.of(context).style.color;
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        if (busy)
          SizedBox.square(dimension: 18, child: CircularProgressIndicator(strokeWidth: 2, color: color))
        else if (icon != null)
          Icon(icon, size: 18),
        if (busy || icon != null) const SizedBox(width: 8),
        Flexible(child: Text(busy ? busyLabel : label, overflow: TextOverflow.ellipsis)),
      ],
    );
  }
}

/// "Court ... Rs 1,200": a label and an amount on one line.
class AmountRow extends StatelessWidget {
  const AmountRow({super.key, required this.label, required this.value, this.strong = false, this.valueColor});

  final String label;
  final String value;
  final bool strong;
  final Color? valueColor;

  @override
  Widget build(BuildContext context) {
    final weight = strong ? FontWeight.w600 : FontWeight.w400;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: Text(label, style: TextStyle(color: strong ? null : context.fs.muted, fontWeight: weight)),
          ),
          const SizedBox(width: 12),
          Text(value, style: TextStyle(fontFeatures: tabular, fontWeight: weight, color: valueColor)),
        ],
      ),
    );
  }
}

/// A line of text in an error, notice or success colour, announced to screen readers.
class Notice extends StatelessWidget {
  const Notice(this.text, {super.key, this.color});

  final String text;
  final Color? color;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      liveRegion: true,
      child: Text(text, style: TextStyle(color: color ?? context.fs.danger)),
    );
  }
}

/// A choice between a few options in a grey track, like Upcoming / Past.
class Segmented<T> extends StatelessWidget {
  const Segmented({super.key, required this.options, required this.value, required this.onChanged, this.height = 40});

  final List<(T, String)> options;
  final T value;
  final ValueChanged<T> onChanged;
  final double height;

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    return Container(
      padding: const EdgeInsets.all(4),
      decoration: BoxDecoration(color: c.surface2, borderRadius: BorderRadius.circular(8)),
      child: Row(
        children: [
          for (final (option, label) in options)
            Expanded(
              child: Semantics(
                selected: option == value,
                button: true,
                child: GestureDetector(
                  behavior: HitTestBehavior.opaque,
                  onTap: () => onChanged(option),
                  child: Container(
                    height: height,
                    alignment: Alignment.center,
                    decoration: BoxDecoration(
                      color: option == value ? c.surface : Colors.transparent,
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: Text(
                      label,
                      style: TextStyle(fontWeight: FontWeight.w500, color: option == value ? c.text : c.muted),
                    ),
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }
}

/// A field's label above it, as on the web.
class FieldLabel extends StatelessWidget {
  const FieldLabel(this.text, {super.key, this.optional = false});

  final String text;
  final bool optional;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: Text.rich(
        TextSpan(
          text: text,
          style: const TextStyle(fontWeight: FontWeight.w500),
          children: [
            if (optional)
              TextSpan(text: l.optional, style: TextStyle(fontWeight: FontWeight.w400, color: context.fs.subtle)),
          ],
        ),
      ),
    );
  }
}

/// One of a few options, as a bordered row that shows when it's picked (a membership, a split rule...).
class ChoiceTile extends StatelessWidget {
  const ChoiceTile({super.key, required this.selected, required this.onTap, required this.child});

  final bool selected;
  final VoidCallback onTap;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Semantics(
        inMutuallyExclusiveGroup: true,
        checked: selected,
        button: true,
        child: Material(
          color: c.surface,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(8),
            side: BorderSide(color: selected ? c.ink : c.border, width: selected ? 1.5 : 1),
          ),
          child: InkWell(
            borderRadius: BorderRadius.circular(8),
            onTap: onTap,
            child: Container(
              constraints: const BoxConstraints(minHeight: 44),
              width: double.infinity,
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              alignment: Alignment.centerLeft,
              child: DefaultTextStyle.merge(style: TextStyle(color: selected ? c.text : c.muted), child: child),
            ),
          ),
        ),
      ),
    );
  }
}

/// A rounded filter or choice that fills in ink when picked: a city, a day, a level.
class Pill extends StatelessWidget {
  const Pill({super.key, required this.label, required this.selected, required this.onTap, this.height = 40});

  final String label;
  final bool selected;
  final VoidCallback onTap;
  final double height;

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    return Semantics(
      selected: selected,
      button: true,
      child: Material(
        color: selected ? c.ink : c.surface,
        shape: StadiumBorder(side: BorderSide(color: selected ? c.ink : c.border)),
        child: InkWell(
          customBorder: const StadiumBorder(),
          onTap: onTap,
          child: SizedBox(
            height: height,
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Center(
                widthFactor: 1,
                child: Text(
                  label,
                  style: TextStyle(fontSize: 14, fontWeight: FontWeight.w500, color: selected ? c.inkFg : c.muted),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

/// A heading for a part of a screen, with room for an action beside it.
class SectionTitle extends StatelessWidget {
  const SectionTitle(this.text, {super.key, this.action});

  final String text;
  final Widget? action;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        children: [
          Expanded(child: Semantics(header: true, child: Text(text, style: Theme.of(context).textTheme.titleMedium))),
          ?action,
        ],
      ),
    );
  }
}

/// A labelled group of pills where tapping the chosen one again clears it: a position, a level.
class PillChoices<T> extends StatelessWidget {
  const PillChoices({
    super.key,
    required this.label,
    required this.options,
    required this.value,
    required this.onChanged,
    this.optional = false,
  });

  final String label;
  final bool optional;
  final List<(T, String)> options;
  final T? value;
  final ValueChanged<T?> onChanged;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          FieldLabel(label, optional: optional),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              for (final (option, text) in options)
                Pill(label: text, selected: value == option, onTap: () => onChanged(value == option ? null : option)),
            ],
          ),
        ],
      ),
    );
  }
}
