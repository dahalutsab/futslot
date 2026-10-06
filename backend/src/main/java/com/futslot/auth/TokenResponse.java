package com.futslot.auth;

import com.futslot.user.UserResponse;

record TokenResponse(String accessToken, String tokenType, long expiresInSeconds, String refreshToken,
		UserResponse user) {

}
