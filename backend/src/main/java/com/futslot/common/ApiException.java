package com.futslot.common;

import org.springframework.http.HttpStatus;

/**
 * An error the client can act on. {@code code} is a stable machine-readable identifier
 * (e.g. {@code OTP_INVALID}) returned alongside the human-readable message.
 * <p>
 * The message is English. Other languages word it from i18n/errors_*.properties under "error." + code (see
 * {@link Wording#error}); {@code values} are what that wording fills in (counts, amounts, names, days), and
 * {@link #variant} picks a wording of its own where one code has several messages.
 */
public class ApiException extends RuntimeException {

	private final HttpStatus status;

	private final String code;

	private Object[] values;

	private String key;

	public ApiException(HttpStatus status, String code, String message, Object... values) {
		super(message);
		this.status = status;
		this.code = code;
		this.values = values;
		this.key = "error." + code;
	}

	/** The values other languages fill in, for an exception made by a helper that doesn't take them. */
	public ApiException with(Object... values) {
		this.values = values;
		return this;
	}

	/** Worded in other languages as "error." + code + "." + {@code name}. */
	public ApiException variant(String name) {
		this.key = "error." + this.code + "." + name;
		return this;
	}

	public HttpStatus getStatus() {
		return this.status;
	}

	public String getCode() {
		return this.code;
	}

	/** Where other languages find their wording. */
	public String getKey() {
		return this.key;
	}

	public Object[] getValues() {
		return this.values;
	}

}
