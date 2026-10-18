import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../core/api_types.dart';
import '../core/theme.dart';
import 'format.dart';
import 'widgets.dart';

/// The methods a player can report having paid with (cash is taken at the counter, not reported).
const claimable = [PaymentMethod.esewa, PaymentMethod.khalti, PaymentMethod.fonepay, PaymentMethod.bankTransfer];

/// "I paid": where to send the money, then the method and the transaction ID from the receipt. The amount is
/// fixed unless [minimum] is set, when the player may pay more (up to [amount]).
class PayForm extends StatefulWidget {
  const PayForm({
    super.key,
    required this.amount,
    required this.onPaid,
    this.minimum,
    this.instructions,
    this.label = 'Send for checking',
    this.busy = false,
    this.error,
  });

  /// What to pay: exact, or the most when [minimum] is set.
  final double amount;
  final double? minimum;
  final String? instructions;
  final String label;
  final bool busy;
  final String? error;
  final ValueChanged<PaymentClaim> onPaid;

  @override
  State<PayForm> createState() => _PayFormState();
}

class _PayFormState extends State<PayForm> {
  var _method = PaymentMethod.esewa;

  final _transactionId = TextEditingController();

  late final _paid = TextEditingController(text: _plain(widget.minimum ?? widget.amount));

  String? _local;

  static String _plain(double amount) =>
      amount == amount.roundToDouble() ? amount.toInt().toString() : amount.toStringAsFixed(2);

  @override
  void didUpdateWidget(PayForm old) {
    super.didUpdateWidget(old);
    if (old.minimum != widget.minimum || old.amount != widget.amount) {
      _paid.text = _plain(widget.minimum ?? widget.amount);
    }
  }

  @override
  void dispose() {
    _transactionId.dispose();
    _paid.dispose();
    super.dispose();
  }

  void _submit() {
    if (widget.busy) {
      return;
    }
    final transactionId = _transactionId.text.trim();
    final minimum = widget.minimum;
    final paid = double.tryParse(_paid.text.trim());
    if (transactionId.length < 4) {
      setState(() => _local = 'Enter the transaction ID from your receipt.');
      return;
    }
    if (minimum != null && (paid == null || paid < minimum || paid > widget.amount)) {
      setState(() => _local = 'Enter between ${rupees(minimum)} and ${rupees(widget.amount)}.');
      return;
    }
    setState(() => _local = null);
    FocusScope.of(context).unfocus();
    widget.onPaid(PaymentClaim(method: _method, transactionId: transactionId, amount: minimum != null ? paid! : widget.amount));
  }

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    final minimum = widget.minimum;
    final problem = widget.error ?? _local;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        if (widget.instructions case final instructions?) ...[
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            decoration: BoxDecoration(color: c.surface2, borderRadius: BorderRadius.circular(8)),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Send ${minimum != null ? 'at least ${rupees(minimum)}' : rupees(widget.amount)} to',
                  style: TextStyle(fontSize: 14, color: c.muted),
                ),
                const SizedBox(height: 2),
                SelectableText(instructions, style: const TextStyle(fontWeight: FontWeight.w500)),
              ],
            ),
          ),
          const SizedBox(height: 12),
        ],
        const FieldLabel('Paid with'),
        GridView.count(
          crossAxisCount: 2,
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          mainAxisSpacing: 8,
          crossAxisSpacing: 8,
          childAspectRatio: 3.4,
          children: [
            for (final method in claimable)
              _MethodOption(
                label: methodLabel(method),
                selected: _method == method,
                onTap: () => setState(() => _method = method),
              ),
          ],
        ),
        if (minimum != null) ...[
          const SizedBox(height: 12),
          const FieldLabel('Amount you sent'),
          TextField(
            controller: _paid,
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
            inputFormatters: [FilteringTextInputFormatter.allow(RegExp(r'[0-9.]'))],
            style: const TextStyle(fontFeatures: tabular),
            decoration: const InputDecoration(prefixText: 'Rs '),
          ),
        ],
        const SizedBox(height: 12),
        const FieldLabel('Transaction ID'),
        TextField(
          controller: _transactionId,
          maxLength: 100,
          autocorrect: false,
          enableSuggestions: false,
          style: const TextStyle(fontFeatures: tabular),
          textInputAction: TextInputAction.done,
          onSubmitted: (_) => _submit(),
          decoration: const InputDecoration(hintText: 'From your payment receipt', counterText: ''),
        ),
        if (problem != null) ...[const SizedBox(height: 12), Notice(problem)],
        const SizedBox(height: 12),
        FilledButton(
          onPressed: widget.busy ? null : _submit,
          child: BusyLabel(busy: widget.busy, label: widget.label, busyLabel: 'Sending…'),
        ),
      ],
    );
  }
}

class _MethodOption extends StatelessWidget {
  const _MethodOption({required this.label, required this.selected, required this.onTap});

  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    return Semantics(
      inMutuallyExclusiveGroup: true,
      checked: selected,
      button: true,
      child: Material(
        color: selected ? c.surface2 : c.surface,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(8),
          side: BorderSide(color: selected ? c.ink : c.border, width: selected ? 1.5 : 1),
        ),
        child: InkWell(
          borderRadius: BorderRadius.circular(8),
          onTap: onTap,
          child: Center(
            child: Text(
              label,
              style: TextStyle(fontSize: 14, fontWeight: FontWeight.w500, color: selected ? c.text : c.muted),
            ),
          ),
        ),
      ),
    );
  }
}
