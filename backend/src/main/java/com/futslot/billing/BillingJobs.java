package com.futslot.billing;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Hourly: subscription renewals and expiry, featured listings starting and ending. */
@Component
@ConditionalOnBooleanProperty(name = "futslot.billing.jobs.enabled", matchIfMissing = true)
class BillingJobs {

	private final SubscriptionService subscriptions;

	private final FeaturedService featured;

	BillingJobs(SubscriptionService subscriptions, FeaturedService featured) {
		this.subscriptions = subscriptions;
		this.featured = featured;
	}

	@Scheduled(cron = "${futslot.billing.jobs.cron:0 15 * * * *}")
	void run() {
		this.subscriptions.runBilling();
		this.featured.maintain();
	}

}
