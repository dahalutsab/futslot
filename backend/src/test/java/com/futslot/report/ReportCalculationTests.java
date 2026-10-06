package com.futslot.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class ReportCalculationTests {

	@Test
	void openMinutesHandleLateNightAndAllDayHours() {
		assertThat(ReportService.openMinutes(LocalTime.of(6, 0), LocalTime.of(22, 0))).isEqualTo(16 * 60);
		assertThat(ReportService.openMinutes(LocalTime.of(18, 0), LocalTime.of(2, 0))).isEqualTo(8 * 60);
		assertThat(ReportService.openMinutes(LocalTime.MIDNIGHT, LocalTime.MIDNIGHT)).isEqualTo(24 * 60);
	}

	@Test
	void csvCellsAreQuotedAndFormulasDefused() {
		assertThat(BookingExport.escape("Court A")).isEqualTo("Court A");
		assertThat(BookingExport.escape("Ram, Sita")).isEqualTo("\"Ram, Sita\"");
		assertThat(BookingExport.escape("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
		assertThat(BookingExport.escape("=1+1")).isEqualTo("'=1+1");
		assertThat(BookingExport.escape("@SUM(A1)")).isEqualTo("'@SUM(A1)");
		assertThat(BookingExport.escape("+9779812345678")).isEqualTo("+9779812345678");
		assertThat(BookingExport.escape("-5.50")).isEqualTo("-5.50");
		assertThat(BookingExport.escape(null)).isEmpty();
	}

}
