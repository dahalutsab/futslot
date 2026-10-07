package com.futslot.booking;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Hourly top-up of weekly bookings; each series in its own transaction so one failure doesn't stop the rest. */
@Component
@ConditionalOnBooleanProperty(name = "futslot.bookings.recurring.enabled", matchIfMissing = true)
class RecurringBookingScheduler {

	private static final Logger logger = LoggerFactory.getLogger(RecurringBookingScheduler.class);

	private final RecurringBookingService service;

	RecurringBookingScheduler(RecurringBookingService service) {
		this.service = service;
	}

	@Scheduled(fixedDelayString = "${futslot.bookings.recurring.interval-ms:3600000}", initialDelay = 60_000)
	void run() {
		for (UUID seriesId : this.service.needingGeneration()) {
			try {
				this.service.extend(seriesId);
			}
			catch (RuntimeException ex) {
				logger.warn("Could not extend weekly booking {}", seriesId, ex);
			}
		}
	}

}
