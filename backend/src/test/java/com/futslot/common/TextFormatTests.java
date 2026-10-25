package com.futslot.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class TextFormatTests {

	@Test
	void writesRupeesWithLakhGrouping() {
		assertThat(TextFormat.ENGLISH.rupees(new BigDecimal("1200.00"))).isEqualTo("Rs 1,200");
		assertThat(TextFormat.ENGLISH.rupees(new BigDecimal("120000"))).isEqualTo("Rs 1,20,000");
		assertThat(TextFormat.ENGLISH.rupees(new BigDecimal("12000000"))).isEqualTo("Rs 1,20,00,000");
		assertThat(TextFormat.ENGLISH.rupees(new BigDecimal("950"))).isEqualTo("Rs 950");
		assertThat(TextFormat.ENGLISH.rupees(new BigDecimal("1500.50"))).isEqualTo("Rs 1,500.5");
	}

	@Test
	void writesTimesTheWayPeopleSayThem() {
		assertThat(TextFormat.ENGLISH.clock(LocalTime.of(18, 0))).isEqualTo("18:00");
		assertThat(TextFormat.ENGLISH.clock(LocalTime.of(6, 30))).isEqualTo("06:30");
		assertThat(TextFormat.ENGLISH.clock(LocalTime.MIDNIGHT)).isEqualTo("00:00");
		assertThat(TextFormat.ENGLISH.span(LocalTime.of(22, 0), LocalTime.of(23, 59, 59))).isEqualTo("22:00 – 00:00");
		assertThat(TextFormat.ENGLISH.day(LocalDate.of(2026, 10, 9))).isEqualTo("Fri 9 Oct");
	}

	@Test
	void writesNepaliWithDevanagariDigitsAndBikramSambatDates() {
		TextFormat nepali = TextFormat.NEPALI;
		assertThat(nepali.rupees(new BigDecimal("120000"))).isEqualTo("रु १,२०,०००");
		assertThat(nepali.clock(LocalTime.of(6, 30))).isEqualTo("बिहान ६:३० बजे");
		assertThat(nepali.clock(LocalTime.of(13, 0))).isEqualTo("दिउँसो १ बजे");
		assertThat(nepali.clock(LocalTime.of(18, 0))).isEqualTo("बेलुका ६ बजे");
		assertThat(nepali.span(LocalTime.of(22, 0), LocalTime.of(23, 59, 59))).isEqualTo("राति १० बजे – राति १२ बजे");
		// 21 October 2026 is 4 Kartik 2083, a Wednesday
		assertThat(nepali.day(LocalDate.of(2026, 10, 21))).isEqualTo("बुध, ४ कार्तिक");
		assertThat(nepali.dayOfWeek(LocalDate.of(2026, 10, 21))).isEqualTo("बुध ४");
		assertThat(nepali.weekday(DayOfWeek.FRIDAY)).isEqualTo("शुक्रबार");
		assertThat(nepali.year(LocalDate.of(2026, 10, 21))).isEqualTo("२०८३");
		assertThat(TextFormat.ENGLISH.dayOfWeek(LocalDate.of(2026, 10, 9))).isEqualTo("Fri 9");
	}

}
