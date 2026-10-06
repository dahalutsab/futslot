package com.futslot.common;

import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;

/**
 * Normalises phone numbers to E.164. Local Nepali mobile numbers (98XXXXXXXX) get the +977 prefix.
 */
public final class PhoneNumbers {

	private static final Pattern E164 = Pattern.compile("^\\+[1-9][0-9]{6,14}$");

	private static final Pattern NEPAL_MOBILE = Pattern.compile("^9[678][0-9]{8}$");

	private PhoneNumbers() {
	}

	public static String normalize(String input) {
		String phone = (input == null) ? "" : input.replaceAll("[\\s()-]", "");
		if (NEPAL_MOBILE.matcher(phone).matches()) {
			phone = "+977" + phone;
		}
		else if (phone.startsWith("00")) {
			phone = "+" + phone.substring(2);
		}
		else if (phone.startsWith("977")) {
			phone = "+" + phone;
		}
		if (!E164.matcher(phone).matches()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PHONE", "Enter a valid mobile number");
		}
		return phone;
	}

}
