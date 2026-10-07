package com.futslot.billing;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * FutSlot's own billing. {@code paymentInstructions} tells venues where to pay FutSlot (an eSewa number
 * or bank account) until a gateway is connected. {@code enforceLimits} switches plan limits on.
 */
@ConfigurationProperties("futslot.subscriptions")
record BillingProperties(@DefaultValue("false") boolean enforceLimits,
		@DefaultValue("Contact FutSlot for payment details") String paymentInstructions,
		@DefaultValue("7") int renewalNoticeDays, @DefaultValue("7") int graceDays,
		Map<FeaturedPlacement, Placement> featured) {

	/** {@code maxLive} featured venues at once (per city for CITY_TOP). */
	record Placement(BigDecimal pricePerDay, int maxLive) {

	}

	Placement placement(FeaturedPlacement placement) {
		Placement configured = (this.featured != null) ? this.featured.get(placement) : null;
		if (configured == null) {
			throw new IllegalStateException("No price configured for futslot.subscriptions.featured." + placement);
		}
		return configured;
	}

}
