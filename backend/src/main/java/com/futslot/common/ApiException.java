package com.futslot.common;

import org.springframework.http.HttpStatus;

/**
 * An error the client can act on. {@code code} is a stable machine-readable identifier
 * (e.g. {@code OTP_INVALID}) returned alongside the human-readable message.
 */
public class ApiException extends RuntimeException {

	private final HttpStatus status;

	private final String code;

	public ApiException(HttpStatus status, String code, String message) {
		super(message);
		this.status = status;
		this.code = code;
	}

	public HttpStatus getStatus() {
		return this.status;
	}

	public String getCode() {
		return this.code;
	}

}
