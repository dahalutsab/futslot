package com.futslot.auth;

/** {@code code} is the code itself, only when codes are shown on screen (a demo without SMS); null otherwise. */
record OtpSentResponse(long expiresInSeconds, long resendAfterSeconds, String code) {

}
