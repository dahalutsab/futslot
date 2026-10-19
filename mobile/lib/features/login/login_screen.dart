import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/api_error.dart';
import '../../core/api_types.dart';
import '../../core/google.dart';
import '../../core/session.dart';
import '../../core/theme.dart';
import '../../shared/widgets.dart';

/// Sign in with a phone number and password, or with Google; create an account; or reset a forgotten password.
/// Signing in takes the player on to [next], where they were going.
class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key, this.next});

  final String? next;

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

/// signIn: phone and password. The others text a code first: signUp makes an account, reset sets a new password (or
/// the first one, for an account from before passwords), google proves the number on a first Google sign-in.
enum _Mode { signIn, signUp, reset, google }

const _minPassword = 8;

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _phone = TextEditingController();

  final _password = TextEditingController();

  final _code = TextEditingController();

  final _fullName = TextEditingController();

  final _passwordFocus = FocusNode();

  final _codeFocus = FocusNode();

  final _nameFocus = FocusNode();

  var _mode = _Mode.signIn;

  /// For the modes that text a code: whether it's been sent.
  var _codeSent = false;

  var _showPassword = false;

  /// Google's sign-in needs a name typed in only when Google didn't give one.
  var _needsName = false;

  var _busy = false;

  String? _error;

  String? _notice;

  var _resendIn = 0;

  Timer? _timer;

  String? _googleClientId;

  /// The ID token from Google, kept while the player proves their number.
  String? _googleToken;

  late final bool _expired = ref.read(sessionProvider.notifier).takeExpired();

  Session get _session => ref.read(sessionProvider.notifier);

  /// Accepts "98-1234 5678", "+977 9812345678" and so on; the API wants the 10 local digits.
  String get _digits {
    final digits = _phone.text.replaceAll(RegExp(r'\D'), '');
    return digits.startsWith('977') && digits.length == 13 ? digits.substring(3) : digits;
  }

  bool get _phoneValid => RegExp(r'^\d{10}$').hasMatch(_digits);

  @override
  void initState() {
    super.initState();
    _session.options().then((options) {
      if (mounted) {
        setState(() => _googleClientId = options.googleWebClientId);
      }
    }, onError: (_) {});
  }

  @override
  void dispose() {
    _timer?.cancel();
    for (final field in [_phone, _password, _code, _fullName]) {
      field.dispose();
    }
    for (final focus in [_passwordFocus, _codeFocus, _nameFocus]) {
      focus.dispose();
    }
    super.dispose();
  }

  void _switchTo(_Mode mode, [String? notice]) {
    setState(() {
      _mode = mode;
      _codeSent = false;
      _code.clear();
      _password.clear();
      _needsName = false;
      _error = null;
      _notice = notice;
    });
  }

  Future<void> _signIn() async {
    if (_busy) {
      return;
    }
    if (!_phoneValid) {
      setState(() => _error = 'Enter your 10-digit mobile number, like 98XXXXXXXX.');
      return;
    }
    if (_password.text.isEmpty) {
      setState(() => _error = 'Enter your password.');
      return;
    }
    await _run(() => _session.signIn(_digits, _password.text), (code) {
      if (code == 'PASSWORD_NOT_SET') {
        _switchTo(_Mode.reset, "Your account doesn't have a password yet. We'll text you a code to set one.");
        return true;
      }
      return false;
    });
  }

  Future<void> _sendCode() async {
    if (_busy || _mode == _Mode.signIn) {
      return;
    }
    if (!_phoneValid) {
      setState(() => _error = 'Enter your 10-digit mobile number, like 98XXXXXXXX.');
      return;
    }
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      final purpose = switch (_mode) {
        _Mode.reset => CodePurpose.resetPassword,
        _Mode.google => CodePurpose.linkGoogle,
        _ => CodePurpose.signUp,
      };
      final sent = await _session.requestCode(_digits, purpose);
      _showCodeStep(sent.resendAfterSeconds);
    } catch (error) {
      final problem = problemOf(error);
      switch (problem.code) {
        case 'OTP_COOLDOWN':
          // A code went out under a minute ago (a double tap, or coming back): let them use it
          _notice = 'We sent you a code a moment ago. Enter it below.';
          _showCodeStep(60);
        case 'ACCOUNT_EXISTS':
          _switchTo(
            _Mode.signIn,
            'This number already has an account. Sign in, or reset your password if you forgot it.',
          );
        case 'ACCOUNT_NOT_FOUND':
          _switchTo(_Mode.signUp, 'No account uses this number yet. Create one below.');
        default:
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
    setState(() => _codeSent = true);
    _startCountdown(resendAfter);
    WidgetsBinding.instance.addPostFrameCallback((_) => _codeFocus.requestFocus());
  }

  /// The code step: create the account, set the new password, or link Google.
  Future<void> _finish() async {
    if (_busy || _mode == _Mode.signIn) {
      return;
    }
    final name = _fullName.text.trim();
    final problem = !RegExp(r'^\d{6}$').hasMatch(_code.text)
        ? 'Enter the 6-digit code we sent.'
        : (_mode == _Mode.signUp || _needsName) && name.length < 2
            ? 'Enter your name.'
            : _mode != _Mode.google && _password.text.runes.length < _minPassword
                ? 'Use at least $_minPassword characters for your password.'
                : null;
    if (problem != null) {
      setState(() => _error = problem);
      return;
    }
    await _run(
      () => switch (_mode) {
        _Mode.signUp => _session.signUp(_digits, _code.text, name, _password.text),
        _Mode.reset => _session.resetPassword(_digits, _code.text, _password.text),
        _ => _session.linkGoogle(_googleToken!, _digits, _code.text, name),
      },
      (code) {
        if (code == 'FULL_NAME_REQUIRED') {
          setState(() => _needsName = true);
          WidgetsBinding.instance.addPostFrameCallback((_) => _nameFocus.requestFocus());
          return true;
        }
        return false;
      },
    );
  }

  Future<void> _google() async {
    final clientId = _googleClientId;
    if (_busy || clientId == null) {
      return;
    }
    setState(() => _error = null);
    String? token;
    try {
      token = await ref.read(googleIdTokenProvider)(clientId);
    } catch (_) {
      setState(() => _error = "Google sign-in didn't work. Use your number and password instead.");
      return;
    }
    if (token == null) {
      return;
    }
    _googleToken = token;
    await _run(() => _session.google(token!), (code) {
      if (code == 'GOOGLE_PHONE_NEEDED') {
        _switchTo(_Mode.google);
        return true;
      }
      return false;
    });
  }

  /// Signs in with [action], then goes where the player was headed. [handled] deals with known codes.
  Future<void> _run(Future<User> Function() action, bool Function(String? code) handled) async {
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await action();
      // Sign-in may have been pushed over another screen, where the router's redirect doesn't reach
      if (mounted) {
        context.go(widget.next ?? '/');
      }
    } catch (error) {
      final problem = problemOf(error);
      if (!handled(problem.code) && mounted) {
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
      _codeSent = false;
      _code.clear();
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
    final (title, subtitle) = switch (_mode) {
      _Mode.signIn => ('Sign in to FutSlot', 'Use your mobile number and password.'),
      _Mode.signUp => ('Create your account', "We'll text a code to check the number is yours."),
      _Mode.reset => ('Reset your password', "We'll text a code to your number, then you choose a new password."),
      _Mode.google => (
          'One more step',
          "Add your mobile number. Venues and teammates find you by it, so we'll text a code to check it's yours."
        ),
    };
    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          tooltip: 'Back to venues',
          icon: const Icon(LucideIcons.x),
          onPressed: () => context.canPop() ? context.pop() : context.go('/'),
        ),
      ),
      body: SafeArea(
        child: AutofillGroup(
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
              Text(title, style: Theme.of(context).textTheme.headlineSmall),
              const SizedBox(height: 4),
              Text(subtitle, style: TextStyle(color: c.muted)),
              if (_expired && _mode == _Mode.signIn) ...[
                const SizedBox(height: 16),
                _Banner(text: 'You were signed out. Please sign in again.', colors: c.held),
              ],
              if (_notice case final notice?) ...[
                const SizedBox(height: 16),
                Semantics(
                  liveRegion: true,
                  child: _Banner(text: notice, colors: StateColors(bg: c.surface2, line: c.border, fg: c.muted)),
                ),
              ],
              const SizedBox(height: 32),
              if (_mode == _Mode.signIn)
                ..._signInForm(c)
              else if (!_codeSent)
                ..._phoneStep(c)
              else
                ..._codeStep(c),
              if (_mode != _Mode.signIn) ...[
                const SizedBox(height: 24),
                Center(child: _LinkButton(label: 'Back to sign in', onTap: () => _switchTo(_Mode.signIn))),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Widget _phoneField(FsColors c, {required TextInputAction action, required VoidCallback onDone}) => TextField(
        controller: _phone,
        autofocus: true,
        keyboardType: TextInputType.phone,
        autofillHints: const [AutofillHints.telephoneNumberNational],
        textInputAction: action,
        onSubmitted: (_) => onDone(),
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
      );

  Widget _passwordField({required bool isNew, required VoidCallback onDone}) => TextField(
        controller: _password,
        focusNode: _passwordFocus,
        obscureText: !_showPassword,
        autocorrect: false,
        enableSuggestions: false,
        autofillHints: [isNew ? AutofillHints.newPassword : AutofillHints.password],
        textInputAction: TextInputAction.go,
        onSubmitted: (_) => onDone(),
        decoration: InputDecoration(
          suffixIcon: IconButton(
            tooltip: _showPassword ? 'Hide password' : 'Show password',
            icon: Icon(_showPassword ? LucideIcons.eyeOff : LucideIcons.eye, size: 18),
            onPressed: () => setState(() => _showPassword = !_showPassword),
          ),
        ),
      );

  List<Widget> _signInForm(FsColors c) => [
        const FieldLabel('Mobile number'),
        _phoneField(c, action: TextInputAction.next, onDone: _passwordFocus.requestFocus),
        const SizedBox(height: 16),
        Row(
          children: [
            const Expanded(child: FieldLabel('Password')),
            _LinkButton(label: 'Forgot password?', onTap: () => _switchTo(_Mode.reset)),
          ],
        ),
        _passwordField(isNew: false, onDone: _signIn),
        if (_error case final error?) ...[const SizedBox(height: 16), Notice(error)],
        const SizedBox(height: 16),
        FilledButton(
          onPressed: _busy ? null : _signIn,
          child: BusyLabel(busy: _busy, label: 'Sign in', busyLabel: 'Signing in…', icon: LucideIcons.shieldCheck),
        ),
        if (_googleClientId != null) ...[
          const SizedBox(height: 24),
          ExcludeSemantics(
            child: Row(
              children: [
                Expanded(child: Divider(color: c.border)),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 12),
                  child: Text('or', style: TextStyle(fontSize: 14, color: c.subtle)),
                ),
                Expanded(child: Divider(color: c.border)),
              ],
            ),
          ),
          const SizedBox(height: 24),
          OutlinedButton(onPressed: _busy ? null : _google, child: const Text('Continue with Google')),
        ],
        const SizedBox(height: 32),
        Wrap(
          alignment: WrapAlignment.center,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            Text('New to FutSlot?', style: TextStyle(color: c.muted)),
            _LinkButton(label: 'Create an account', onTap: () => _switchTo(_Mode.signUp)),
          ],
        ),
      ];

  List<Widget> _phoneStep(FsColors c) => [
        const FieldLabel('Mobile number'),
        _phoneField(c, action: TextInputAction.go, onDone: _sendCode),
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
          textInputAction: TextInputAction.next,
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
        if (_mode == _Mode.signUp || _needsName) ...[
          const SizedBox(height: 16),
          const FieldLabel('Your name'),
          TextField(
            controller: _fullName,
            focusNode: _nameFocus,
            textCapitalization: TextCapitalization.words,
            autofillHints: const [AutofillHints.name],
            textInputAction: TextInputAction.next,
          ),
          const SizedBox(height: 6),
          Text('Venues see this name on your bookings.', style: TextStyle(fontSize: 14, color: c.subtle)),
        ],
        if (_mode != _Mode.google) ...[
          const SizedBox(height: 16),
          FieldLabel(_mode == _Mode.reset ? 'New password' : 'Password'),
          _passwordField(isNew: true, onDone: _finish),
          const SizedBox(height: 6),
          Text(
            'At least $_minPassword characters. Not your phone number, or something as easy to guess.',
            style: TextStyle(fontSize: 14, color: c.subtle),
          ),
        ],
        if (_error case final error?) ...[const SizedBox(height: 16), Notice(error)],
        const SizedBox(height: 16),
        FilledButton(
          onPressed: _busy ? null : _finish,
          child: BusyLabel(
            busy: _busy,
            label: switch (_mode) {
              _Mode.signUp => 'Create account',
              _Mode.reset => 'Save password and sign in',
              _ => 'Continue',
            },
            busyLabel: 'Checking…',
            icon: LucideIcons.shieldCheck,
          ),
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
