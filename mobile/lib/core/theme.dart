import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// FutSlot's design tokens, the same values as the web apps' styles.css: neutral surfaces and ink buttons, and
/// each booking state has one colour used everywhere (see docs/design/mobile.md). Widgets read them with
/// `context.fs`, never raw colours.
@immutable
class FsColors extends ThemeExtension<FsColors> {
  const FsColors({
    required this.bg,
    required this.surface,
    required this.surface2,
    required this.border,
    required this.text,
    required this.muted,
    required this.subtle,
    required this.ink,
    required this.inkFg,
    required this.ring,
    required this.danger,
    required this.dangerBg,
    required this.brand,
    required this.confirmed,
    required this.held,
    required this.verify,
    required this.blocked,
    required this.noshow,
  });

  final Color bg;
  final Color surface;
  final Color surface2;
  final Color border;
  final Color text;
  final Color muted;
  final Color subtle;
  final Color ink;
  final Color inkFg;
  final Color ring;
  final Color danger;
  final Color dangerBg;
  final Color brand;

  /// Booking states.
  final StateColors confirmed;
  final StateColors held;
  final StateColors verify;
  final StateColors blocked;
  final StateColors noshow;

  static const light = FsColors(
    bg: Color(0xFFF7F7F8),
    surface: Color(0xFFFFFFFF),
    surface2: Color(0xFFF4F4F5),
    border: Color(0xFFE4E4E7),
    text: Color(0xFF18181B),
    muted: Color(0xFF52525B),
    subtle: Color(0xFF71717A),
    ink: Color(0xFF18181B),
    inkFg: Color(0xFFFFFFFF),
    ring: Color(0xFF2563EB),
    danger: Color(0xFFB91C1C),
    dangerBg: Color(0xFFFEF2F2),
    brand: Color(0xFF15803D),
    confirmed: StateColors(bg: Color(0xFFECFDF5), line: Color(0xFF10B981), fg: Color(0xFF065F46)),
    held: StateColors(bg: Color(0xFFFFFBEB), line: Color(0xFFF59E0B), fg: Color(0xFF92400E)),
    verify: StateColors(bg: Color(0xFFF5F3FF), line: Color(0xFF8B5CF6), fg: Color(0xFF5B21B6)),
    blocked: StateColors(bg: Color(0xFFF4F4F5), line: Color(0xFFA1A1AA), fg: Color(0xFF3F3F46)),
    noshow: StateColors(bg: Color(0xFFFEF2F2), line: Color(0xFFEF4444), fg: Color(0xFF991B1B)),
  );

  /// A cool, very dark grey rather than black.
  static const dark = FsColors(
    bg: Color(0xFF0F1012),
    surface: Color(0xFF17181B),
    surface2: Color(0xFF1F2125),
    border: Color(0xFF2C2E33),
    text: Color(0xFFECECEF),
    muted: Color(0xFFA1A1AA),
    subtle: Color(0xFF8B8B93),
    ink: Color(0xFFFAFAFA),
    inkFg: Color(0xFF18181B),
    ring: Color(0xFF60A5FA),
    danger: Color(0xFFF87171),
    dangerBg: Color(0x1FF87171),
    brand: Color(0xFF4ADE80),
    confirmed: StateColors(bg: Color(0x2410B981), line: Color(0xFF34D399), fg: Color(0xFF6EE7B7)),
    held: StateColors(bg: Color(0x24F59E0B), line: Color(0xFFFBBF24), fg: Color(0xFFFCD34D)),
    verify: StateColors(bg: Color(0x298B5CF6), line: Color(0xFFA78BFA), fg: Color(0xFFC4B5FD)),
    blocked: StateColors(bg: Color(0x1FA1A1AA), line: Color(0xFF71717A), fg: Color(0xFFD4D4D8)),
    noshow: StateColors(bg: Color(0x24EF4444), line: Color(0xFFF87171), fg: Color(0xFFFCA5A5)),
  );

  @override
  FsColors copyWith() => this;

  @override
  FsColors lerp(FsColors? other, double t) => t < 0.5 || other == null ? this : other;
}

/// A state's fill, its border and accent, and text on the fill.
@immutable
class StateColors {
  const StateColors({required this.bg, required this.line, required this.fg});

  final Color bg;
  final Color line;
  final Color fg;
}

extension FsTheme on BuildContext {
  FsColors get fs => Theme.of(this).extension<FsColors>()!;
}

/// Numbers that line up in columns and don't shift as they change.
const tabular = [FontFeature.tabularFigures()];

ThemeData buildTheme(Brightness brightness) {
  final c = brightness == Brightness.light ? FsColors.light : FsColors.dark;
  final radius = BorderRadius.circular(8);
  final base = ThemeData(
    brightness: brightness,
    useMaterial3: true,
    fontFamily: 'Inter',
    fontFamilyFallback: const ['NotoSansDevanagari'],
    colorScheme: ColorScheme.fromSeed(
      seedColor: c.brand,
      brightness: brightness,
      primary: c.ink,
      onPrimary: c.inkFg,
      surface: c.surface,
      onSurface: c.text,
      error: c.danger,
      outline: c.border,
    ),
  );
  final text = base.textTheme.apply(bodyColor: c.text, displayColor: c.text);
  return base.copyWith(
    scaffoldBackgroundColor: c.bg,
    extensions: [c],
    textTheme: text.copyWith(
      headlineSmall: text.headlineSmall?.copyWith(fontSize: 24, fontWeight: FontWeight.w600, letterSpacing: -0.4),
      titleLarge: text.titleLarge?.copyWith(fontSize: 18, fontWeight: FontWeight.w600),
      titleMedium: text.titleMedium?.copyWith(fontSize: 16, fontWeight: FontWeight.w600),
      bodyLarge: text.bodyLarge?.copyWith(fontSize: 16, height: 1.5),
      bodyMedium: text.bodyMedium?.copyWith(fontSize: 16, height: 1.5),
      bodySmall: text.bodySmall?.copyWith(fontSize: 14, height: 1.45, color: c.muted),
      labelLarge: text.labelLarge?.copyWith(fontSize: 16, fontWeight: FontWeight.w500),
    ),
    appBarTheme: AppBarTheme(
      backgroundColor: c.bg,
      foregroundColor: c.text,
      surfaceTintColor: Colors.transparent,
      elevation: 0,
      scrolledUnderElevation: 0,
      centerTitle: false,
      titleTextStyle: TextStyle(fontFamily: 'Inter', fontSize: 18, fontWeight: FontWeight.w600, color: c.text),
    ),
    dividerTheme: DividerThemeData(color: c.border, thickness: 1, space: 1),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        backgroundColor: c.ink,
        foregroundColor: c.inkFg,
        disabledBackgroundColor: c.ink.withValues(alpha: 0.5),
        disabledForegroundColor: c.inkFg,
        minimumSize: const Size(64, 48),
        shape: RoundedRectangleBorder(borderRadius: radius),
        textStyle: const TextStyle(fontFamily: 'Inter', fontSize: 16, fontWeight: FontWeight.w500),
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        foregroundColor: c.text,
        side: BorderSide(color: c.border),
        minimumSize: const Size(44, 44),
        shape: RoundedRectangleBorder(borderRadius: radius),
        textStyle: const TextStyle(fontFamily: 'Inter', fontSize: 16, fontWeight: FontWeight.w500),
      ),
    ),
    textButtonTheme: TextButtonThemeData(
      style: TextButton.styleFrom(
        foregroundColor: c.text,
        minimumSize: const Size(44, 44),
        shape: RoundedRectangleBorder(borderRadius: radius),
        textStyle: const TextStyle(fontFamily: 'Inter', fontSize: 16, fontWeight: FontWeight.w500),
      ),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: c.surface,
      isDense: true,
      contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 13),
      hintStyle: TextStyle(color: c.subtle),
      border: OutlineInputBorder(borderRadius: radius, borderSide: BorderSide(color: c.border)),
      enabledBorder: OutlineInputBorder(borderRadius: radius, borderSide: BorderSide(color: c.border)),
      focusedBorder: OutlineInputBorder(borderRadius: radius, borderSide: BorderSide(color: c.ring, width: 2)),
      errorBorder: OutlineInputBorder(borderRadius: radius, borderSide: BorderSide(color: c.danger)),
    ),
    navigationBarTheme: NavigationBarThemeData(
      backgroundColor: c.surface,
      indicatorColor: c.surface2,
      surfaceTintColor: Colors.transparent,
      height: 64,
      iconTheme: WidgetStateProperty.resolveWith(
        (states) => IconThemeData(size: 22, color: states.contains(WidgetState.selected) ? c.text : c.subtle),
      ),
      labelTextStyle: WidgetStateProperty.resolveWith(
        (states) => TextStyle(
          fontFamily: 'Inter',
          fontSize: 12,
          fontWeight: FontWeight.w500,
          color: states.contains(WidgetState.selected) ? c.text : c.subtle,
        ),
      ),
    ),
    progressIndicatorTheme: ProgressIndicatorThemeData(color: c.ink),
    snackBarTheme: SnackBarThemeData(
      behavior: SnackBarBehavior.floating,
      backgroundColor: c.ink,
      contentTextStyle: TextStyle(fontFamily: 'Inter', color: c.inkFg, fontSize: 15),
    ),
  );
}

enum ThemeChoice { light, dark, system }

final preferencesProvider = Provider<SharedPreferences>((ref) => throw UnimplementedError('Set in main()'));

final themeChoiceProvider = NotifierProvider<ThemeChoiceNotifier, ThemeChoice>(ThemeChoiceNotifier.new);

/// Light, dark, or whatever the phone uses; remembered on the phone.
class ThemeChoiceNotifier extends Notifier<ThemeChoice> {
  static const _key = 'futslot.theme';

  @override
  ThemeChoice build() {
    final saved = ref.read(preferencesProvider).getString(_key);
    return ThemeChoice.values.where((choice) => choice.name == saved).firstOrNull ?? ThemeChoice.system;
  }

  void set(ThemeChoice choice) {
    state = choice;
    ref.read(preferencesProvider).setString(_key, choice.name);
  }
}

ThemeMode themeModeOf(ThemeChoice choice) => switch (choice) {
      ThemeChoice.light => ThemeMode.light,
      ThemeChoice.dark => ThemeMode.dark,
      ThemeChoice.system => ThemeMode.system,
    };
