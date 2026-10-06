package com.futslot.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class PhoneNumbersTests {

	@ParameterizedTest
	@CsvSource({ "9812345678, +9779812345678", "981-234-5678, +9779812345678", "9779812345678, +9779812345678",
			"+977 981 2345678, +9779812345678", "00447700900123, +447700900123", "+447700900123, +447700900123" })
	void normalizesToE164(String input, String expected) {
		assertThat(PhoneNumbers.normalize(input)).isEqualTo(expected);
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "12345", "abc", "+0123456789", "98123" })
	void rejectsInvalidNumbers(String input) {
		assertThatThrownBy(() -> PhoneNumbers.normalize(input)).isInstanceOf(ApiException.class)
			.hasMessage("Enter a valid mobile number");
	}

}
