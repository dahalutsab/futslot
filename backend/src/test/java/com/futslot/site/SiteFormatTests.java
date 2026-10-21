package com.futslot.site;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SiteFormatTests {

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
