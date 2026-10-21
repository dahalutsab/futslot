import 'package:flutter/material.dart';

import '../core/theme.dart';
import '../l10n/l10n.dart';
import 'widgets.dart';

List<String> get _words => ['', l.ratingPoor, l.ratingNotGreat, l.ratingOkay, l.ratingGood, l.ratingGreat];

/// Stars (required) and a comment (optional). [extra] sits beside the save button, e.g. "Cancel" when editing.
class ReviewForm extends StatefulWidget {
  const ReviewForm({
    super.key,
    required this.onSaved,
    this.rating = 0,
    this.comment,
    this.label,
    this.busy = false,
    this.error,
    this.extra,
  });

  final int rating;
  final String? comment;

  /// The button; "Post review" unless set.
  final String? label;
  final bool busy;
  final String? error;
  final Widget? extra;
  final void Function(int rating, String? comment) onSaved;

  @override
  State<ReviewForm> createState() => _ReviewFormState();
}

class _ReviewFormState extends State<ReviewForm> {
  late var _rating = widget.rating;

  late final _comment = TextEditingController(text: widget.comment ?? '');

  String? _local;

  @override
  void dispose() {
    _comment.dispose();
    super.dispose();
  }

  void _submit() {
    if (widget.busy) {
      return;
    }
    if (_rating == 0) {
      setState(() => _local = l.pickStars);
      return;
    }
    setState(() => _local = null);
    FocusScope.of(context).unfocus();
    final comment = _comment.text.trim();
    widget.onSaved(_rating, comment.isEmpty ? null : comment);
  }

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    final problem = widget.error ?? _local;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Row(
          children: [
            for (var n = 1; n <= 5; n++)
              Semantics(
                inMutuallyExclusiveGroup: true,
                checked: _rating == n,
                label: l.starsOutOfFiveWord(digits(n), _words[n]),
                // Excluding the button's own semantics drops its tap, so screen readers pick a star through this
                onTap: () => setState(() => _rating = n),
                excludeSemantics: true,
                child: SizedBox.square(
                  dimension: 44,
                  child: IconButton(
                    padding: EdgeInsets.zero,
                    onPressed: () => setState(() => _rating = n),
                    icon: Icon(
                      n <= _rating ? Icons.star_rounded : Icons.star_outline_rounded,
                      size: 30,
                      color: n <= _rating ? c.held.line : c.subtle,
                    ),
                  ),
                ),
              ),
            const SizedBox(width: 8),
            Text(_words[_rating], style: TextStyle(fontSize: 14, color: c.muted)),
          ],
        ),
        const SizedBox(height: 12),
        FieldLabel(l.whatWasItLike, optional: true),
        TextField(
          controller: _comment,
          maxLength: 1000,
          minLines: 3,
          maxLines: 6,
          textCapitalization: TextCapitalization.sentences,
          decoration: InputDecoration(hintText: l.reviewHint, counterText: ''),
        ),
        if (problem != null) ...[const SizedBox(height: 12), Notice(problem)],
        const SizedBox(height: 12),
        Row(
          children: [
            FilledButton(
              onPressed: widget.busy ? null : _submit,
              style: FilledButton.styleFrom(minimumSize: const Size(96, 44)),
              child: Text(widget.busy ? l.saving : widget.label ?? l.postReview),
            ),
            if (widget.extra != null) ...[const SizedBox(width: 8), widget.extra!],
          ],
        ),
      ],
    );
  }
}

/// "★★★★☆", read out as "4 out of 5".
class Stars extends StatelessWidget {
  const Stars(this.rating, {super.key});

  final int rating;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: l.starsOutOfFive(digits(rating)),
      excludeSemantics: true,
      child: Text('★' * rating + '☆' * (5 - rating), style: TextStyle(color: context.fs.held.line)),
    );
  }
}
