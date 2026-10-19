package com.futslot.auth;

/** What sign-in offers. {@code googleWebClientId} is for "Sign in with Google", and null when it's off. */
record AuthOptions(String googleWebClientId) {

}
