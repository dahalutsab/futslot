import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_sign_in/google_sign_in.dart';

/// Google's sign-in sheet: the ID token to send to the API, or null if the player backed out. [webClientId] comes
/// from the API; Android asks for tokens for it (as the server client), which is what the API accepts.
typedef GoogleIdToken = Future<String?> Function(String webClientId);

final googleIdTokenProvider = Provider<GoogleIdToken>((ref) => _signIn);

String? _initializedFor;

Future<String?> _signIn(String webClientId) async {
  final google = GoogleSignIn.instance;
  if (_initializedFor != webClientId) {
    await google.initialize(serverClientId: webClientId);
    _initializedFor = webClientId;
  }
  try {
    final account = await google.authenticate();
    return account.authentication.idToken;
  } on GoogleSignInException catch (error) {
    if (error.code == GoogleSignInExceptionCode.canceled) {
      return null;
    }
    rethrow;
  }
}
