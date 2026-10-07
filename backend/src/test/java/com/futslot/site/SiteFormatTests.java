package com.futslot.site;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class SiteFormatTests {

	@Test
	void writesRupeesWithLakhGrouping() {
		assertThat(SiteFormat.rupees(new BigDecimal("1200.00"))).isEqualTo("Rs 1,200");
		assertThat(SiteFormat.rupees(new BigDecimal("120000"))).isEqualTo("Rs 1,20,000");
		assertThat(SiteFormat.rupees(new BigDecimal("12000000"))).isEqualTo("Rs 1,20,00,000");
		assertThat(SiteFormat.rupees(new BigDecimal("950"))).isEqualTo("Rs 950");
		assertThat(SiteFormat.rupees(new BigDecimal("1500.50"))).isEqualTo("Rs 1,500.5");
	}

	@Test
	void writesTimesTheWayPeopleSayThem() {
		assertThat(SiteFormat.clock(LocalTime.of(18, 0))).isEqualTo("6 PM");
		assertThat(SiteFormat.clock(LocalTime.of(6, 30))).isEqualTo("6:30 AM");
		assertThat(SiteFormat.clock(LocalTime.MIDNIGHT)).isEqualTo("12 AM");
		assertThat(SiteFormat.span(LocalTime.of(22, 0), LocalTime.of(23, 59, 59))).isEqualTo("10 PM – 12 AM");
		assertThat(SiteFormat.day(LocalDate.of(2026, 10, 9))).isEqualTo("Fri 9 Oct");
	}

	@Test
	void picksReadableTextForTheAccent() {
		assertThat(SiteFormat.inkOn("#c6f432")).isEqualTo("#0b0b0b");
		assertThat(SiteFormat.inkOn("#15803d")).isEqualTo("#ffffff");
		assertThat(SiteFormat.inkOn("#ffffff")).isEqualTo("#0b0b0b");
	}


	@Test
	void adjustsTheAccentUntilItsTextIsReadable() {
		// Bright lime is fine on the dark template but needs darkening on white
		assertThat(SiteFormat.readableOn("#c6f432", "#131715")).isEqualTo("#c6f432");
		assertThat(SiteFormat.readableOn("#c6f432", "#ffffff")).isNotEqualTo("#c6f432");
		// Dark blue is fine on white but needs lightening on the dark template
		assertThat(SiteFormat.readableOn("#1d4ed8", "#ffffff")).isEqualTo("#1d4ed8");
		assertThat(SiteFormat.readableOn("#1d4ed8", "#131715")).isNotEqualTo("#1d4ed8");
	}

}
