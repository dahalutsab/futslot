package com.futslot.deposit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Daily: once a month has ended, its deposit fees become payouts to the venues. */
@Component
@ConditionalOnBooleanProperty(name = "futslot.deposit.payouts.enabled", matchIfMissing = true)
class PayoutJobs {

	private static final Logger logger = LoggerFactory.getLogger(PayoutJobs.class);

	private final PayoutService payouts;

	PayoutJobs(PayoutService payouts) {
		this.payouts = payouts;
	}

	// Daily rather than on the 1st, so a missed run catches up the next day
	@Scheduled(cron = "${futslot.deposit.payouts.cron:0 30 0 * * *}", zone = "Asia/Kathmandu")
	void run() {
		int created = this.payouts.closeEndedMonths();
		if (created > 0) {
			logger.info("Deposit fee payouts: {} created", created);
		}
	}

}
