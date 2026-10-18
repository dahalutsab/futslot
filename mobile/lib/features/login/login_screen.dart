import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/api_error.dart';
import '../../core/session.dart';
import '../../core/theme.dart';
import '../../shared/widgets.dart';

/// Sign in with a phone number and the 6-digit code sent to it. New numbers are asked for a name. Signing in
/// takes the player on to [next], where they were going.
class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key, this.next});

  final String? next;

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

enum _Step { phone, code }

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _phone = TextEditingController();

  final _code = TextEditingController();

  final _fullName = TextEditingController();

  final _codeFocus = FocusNode();

  final _nameFocus = FocusNode();

  var _step = _Step.phone;

  var _needsName = false;

  var _busy = false;

  String? _error;

  String? _notice;

  var _resendIn = 0;

  Timer? _timer;

  late final bool _expired = ref.read(sessionProvider.notifier).takeExpired();

  /// Accepts "98-1234 5678", "+977 9812345678" and so on; the API wants the 10 local digits.
  String get _digits {
    final digits = _phone.text.replaceAll(RegExp(r'\D'), '');
    return digits.startsWith('977') && digits.length == 13 ? digits.substring(3) : digits;
  }

  @override
  void dispose() {
    _timer?.cancel();
    _phone.dispose();
    _code.dispose();
    _fullName.dispose();
    _codeFocus.dispose();
    _nameFocus.dispose();
    super.dispose();
  }

  Future<void> _sendCode() async {
    if (_busy) {
      return;
    }
    if (!RegExp(r'^\d{10}$').hasMatch(_digits)) {
      setState(() => _error = 'Enter your 10-digit mobile number, like 98XXXXXXXX.');
      return;
    }
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      final sent = await ref.read(sessionProvider.notifier).requestCode(_digits);
      _showCodeStep(sent.resendAfterSeconds);
    } catch (error) {
      final problem = problemOf(error);
      if (problem.code == 'OTP_COOLDOWN') {
        // A code went out under a minute ago (a double tap, or coming back): let them use it
        _notice = 'We sent you a code a moment ago. Enter it below.';
        _showCodeStep(60);
      } else {
        setState(() => _error = problem.message);
      }
    } finally {
      if (mounted) {
        setState(() => _busy = false);
      }
    }
  }

  void _showCodeStep(int resendAfter) {
    if (!mounted) {
      return;
    }
    setState(() => _step = _Step.code);
    _startCountdown(resendAfter);
    WidgetsBinding.instance.addPostFrameCallback((_) => _codeFocus.requestFocus());
  }

  Future<void> _verify() async {
    if (_busy) {
      return;
    }
    final name = _fullName.text.trim();
    if (!RegExp(r'^\d{6}$').hasMatch(_code.text) || (_needsName && name.length < 2)) {
      setState(() => _error = _needsName && name.length < 2 ? 'Enter your name.' : 'Enter the 6-digit code we sent.');
      return;
    }
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await ref.read(sessionProvider.notifier).verify(_digits, _code.text, name);
      // Sign-in may have been pushed over another screen, where the router's redirect doesn't reach
      if (mounted) {
        context.go(widget.next ?? '/');
      }
    } catch (error) {
      final problem = problemOf(error);
      if (problem.code == 'FULL_NAME_REQUIRED') {
        setState(() {
          _needsName = true;
          _error = null;
        });
        WidgetsBinding.instance.addPostFrameCallback((_) => _nameFocus.requestFocus());
      } else {
        setState(() => _error = problem.message);
      }
    } finally {
      if (mounted) {
        setState(() => _busy = false);
      }
    }
  }

  void _changeNumber() {
    setState(() {
      _notice = null;
      _step = _Step.phone;
      _code.clear();
      _needsName = false;
      _error = null;
    });
  }

  void _startCountdown(int seconds) {
    _timer?.cancel();
    setState(() => _resendIn = seconds);
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      setState(() => _resendIn = _resendIn > 0 ? _resendIn - 1 : 0);
      if (_resendIn == 0) {
        timer.cancel();
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          tooltip: 'Back to venues',
          icon: const Icon(LucideIcons.x),
          onPressed: () => context.canPop() ? context.pop() : context.go('/'),
        ),
      ),
      body: SafeArea(
        child: ScreenBody(
          maxWidth: 384,
          children: [
            const SizedBox(height: 8),
            Row(
              children: [
                Container(
                  width: 32,
                  height: 32,
                  alignment: Alignment.center,
                  decoration: BoxDecoration(color: c.brand, borderRadius: BorderRadius.circular(6)),
                  child: const Text('F', style: TextStyle(color: Colors.white, fontWeight: FontWeight.w700)),
                ),
                const SizedBox(width: 8),
                const Text('FutSlot', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600)),
              ],
            ),
            const SizedBox(height: 32),
            Text('Sign in to FutSlot', style: Theme.of(context).textTheme.headlineSmall),
            const SizedBox(height: 4),
            Text("We'll text a code to your phone. No password needed.", style: TextStyle(color: c.muted)),
            if (_expired) ...[
              const SizedBox(height: 16),
              _Banner(text: 'You were signed out. Please sign in again.', colors: c.held),
            ],
            const SizedBox(height: 32),
            if (_step == _Step.phone) ..._phoneStep(c) else ..._codeStep(c),
          ],
        ),
      ),
    );
  }

  List<Widget> _phoneStep(FsColors c) => [
        const FieldLabel('Mobile number'),
        TextField(
          controller: _phone,
          autofocus: true,
          keyboardType: TextInputType.phone,
          autofillHints: const [AutofillHints.telephoneNumberNational],
          textInputAction: TextInputAction.go,
          onSubmitted: (_) => _sendCode(),
          style: const TextStyle(fontSize: 16, fontFeatures: tabular),
          decoration: InputDecoration(
            hintText: '98XXXXXXXX',
            prefixIcon: Container(
              width: 64,
              alignment: Alignment.center,
              margin: const EdgeInsets.only(right: 8),
              decoration: BoxDecoration(border: Border(right: BorderSide(color: c.border))),
              child: Text('+977', style: TextStyle(color: c.muted, fontFeatures: tabular)),
            ),
          ),
        ),
        const SizedBox(height: 6),
        Text('New here? The same number signs you up.', style: TextStyle(fontSize: 14, color: c.subtle)),
        if (_error case final error?) ...[const SizedBox(height: 16), Notice(error)],
        const SizedBox(height: 16),
        FilledButton(
          onPressed: _busy ? null : _sendCode,
          child: BusyLabel(busy: _busy, label: 'Send code', busyLabel: 'Sending…'),
        ),
      ];

  List<Widget> _codeStep(FsColors c) => [
        const FieldLabel('6-digit code'),
        TextField(
          controller: _code,
          focusNode: _codeFocus,
          keyboardType: TextInputType.number,
          autofillHints: const [AutofillHints.oneTimeCode],
          inputFormatters: [FilteringTextInputFormatter.digitsOnly, LengthLimitingTextInputFormatter(6)],
          textAlign: TextAlign.center,
          textInputAction: TextInputAction.go,
          onSubmitted: (_) => _verify(),
          style: const TextStyle(fontSize: 20, letterSpacing: 10, fontFeatures: tabular),
        ),
        const SizedBox(height: 6),
        Wrap(
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            Text('Sent to +977 $_digits · ', style: TextStyle(fontSize: 14, color: c.subtle, fontFeatures: tabular)),
            _LinkButton(label: 'Change', onTap: _changeNumber),
          ],
        ),
        if (_notice case final notice?) ...[
          const SizedBox(height: 16),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            decoration: BoxDecoration(color: c.surface2, borderRadius: BorderRadius.circular(8)),
            child: Text(notice, style: TextStyle(color: c.muted)),
          ),
        ],
        if (_needsName) ...[
          const SizedBox(height: 16),
          const FieldLabel('Your name'),
          TextField(
            controller: _fullName,
            focusNode: _nameFocus,
            textCapitalization: TextCapitalization.words,
            autofillHints: const [AutofillHints.name],
            textInputAction: TextInputAction.go,
            onSubmitted: (_) => _verify(),
          ),
          const SizedBox(height: 6),
          Text(
            'This number is new to FutSlot. Venues see this name on your bookings.',
            style: TextStyle(fontSize: 14, color: c.subtle),
          ),
        ],
        if (_error case final error?) ...[const SizedBox(height: 16), Notice(error)],
        const SizedBox(height: 16),
        FilledButton(
          onPressed: _busy ? null : _verify,
          child: BusyLabel(busy: _busy, label: 'Sign in', busyLabel: 'Checking…', icon: LucideIcons.shieldCheck),
        ),
        const SizedBox(height: 8),
        Center(
          child: _resendIn > 0
              ? Padding(
                  padding: const EdgeInsets.symmetric(vertical: 12),
                  child: Text(
                    'Resend code in ${_resendIn}s',
                    style: TextStyle(fontSize: 14, color: c.subtle, fontFeatures: tabular),
                  ),
                )
              : _LinkButton(label: 'Resend code', onTap: _sendCode),
        ),
      ];
}

class _LinkButton extends StatelessWidget {
  const _LinkButton({required this.label, required this.onTap});

  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return TextButton(
      onPressed: onTap,
      style: TextButton.styleFrom(padding: const EdgeInsets.symmetric(horizontal: 4)),
      child: Text(
        label,
        style: const TextStyle(fontSize: 14, decoration: TextDecoration.underline, fontWeight: FontWeight.w500),
      ),
    );
  }
}

class _Banner extends StatelessWidget {
  const _Banner({required this.text, required this.colors});

  final String text;
  final StateColors colors;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(color: colors.bg, borderRadius: BorderRadius.circular(8)),
      child: Text(text, style: TextStyle(color: colors.fg)),
    );
  }
}
