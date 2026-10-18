import 'dart:io';

/// Where the FutSlot API runs. Set it with `--dart-define=API_URL=https://...`; without it, development builds
/// talk to the API on this computer (the Android emulator reaches it at 10.0.2.2).
const _configured = String.fromEnvironment('API_URL');

final String apiBaseUrl = _configured.isNotEmpty
    ? _configured
    : Platform.isAndroid
        ? 'http://10.0.2.2:8080'
        : 'http://localhost:8080';

/// Photos come back as paths on the API ("/media/..."), so they need its address in front.
String mediaUrl(String path) => path.startsWith('http') ? path : '$apiBaseUrl$path';
