package com.futslot.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class NepaliTests {

	@Test
	void writesDevanagariDigits() {
		assertThat(Nepali.digits("Rs 1,250.50")).isEqualTo("Rs १,२५०.५०");
		assertThat(Nepali.digits(2083)).isEqualTo("२०८३");
	}

	@Test
	void agreesWithThePlayerAppsCalendarToTheDay() {
		// Expected dates from nepali-date-converter 3.4.0, which the player web app uses
		assertThat(Nepali.bs(LocalDate.of(1943, 4, 14))).contains(new Nepali.BsDate(2000, 1, 1));
		assertThat(Nepali.bs(LocalDate.of(2024, 1, 1))).contains(new Nepali.BsDate(2080, 9, 16));
		assertThat(Nepali.bs(LocalDate.of(2026, 4, 14))).contains(new Nepali.BsDate(2083, 1, 1));
		assertThat(Nepali.bs(LocalDate.of(2026, 10, 21))).contains(new Nepali.BsDate(2083, 7, 4));
		assertThat(Nepali.bs(LocalDate.of(2030, 12, 31))).contains(new Nepali.BsDate(2087, 9, 15));
		assertThat(Nepali.bs(LocalDate.of(2034, 4, 13))).contains(new Nepali.BsDate(2090, 12, 30));
		assertThat(new Nepali.BsDate(2083, 7, 4).monthName()).isEqualTo("कार्तिक");
	}

	@Test
	void hasNoDateOutsideItsTable() {
		assertThat(Nepali.bs(LocalDate.of(1943, 4, 13))).isEmpty();
		assertThat(Nepali.bs(LocalDate.of(2034, 4, 14))).isEmpty();
	}

}
