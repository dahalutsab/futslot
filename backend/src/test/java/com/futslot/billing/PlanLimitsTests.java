package com.futslot.billing;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.futslot.common.ApiException;
import com.futslot.venue.VenueAccess;
import com.futslot.venue.VenueRepository;

class PlanLimitsTests {

	private final SubscriptionPlanRepository plans = mock(SubscriptionPlanRepository.class);

	private final VenueSubscriptionRepository subscriptions = mock(VenueSubscriptionRepository.class);

	private final UUID venueId = UUID.randomUUID();

	PlanLimitsTests() {
		SubscriptionPlan free = new SubscriptionPlan("FREE",
				new PlanRequest("FREE", "Free", null, BigDecimal.ZERO, null, 1, 0, List.of(), true, 0));
		when(this.plans.findByCode("FREE")).thenReturn(Optional.of(free));
		when(this.subscriptions.findLive(any())).thenReturn(Optional.empty());
	}

	@Test
	void limitsAreIgnoredUntilSwitchedOn() {
		SubscriptionService service = service(false);
		assertThatCode(() -> service.checkCourts(this.venueId, 4)).doesNotThrowAnyException();
		assertThatCode(() -> service.checkStaff(this.venueId, 2)).doesNotThrowAnyException();
	}

	@Test
	void switchedOnTheFreePlanAllowsOneCourtAndNoStaff() {
		SubscriptionService service = service(true);
		assertThatCode(() -> service.checkCourts(this.venueId, 0)).doesNotThrowAnyException();
		assertThatThrownBy(() -> service.checkCourts(this.venueId, 1)).isInstanceOf(ApiException.class)
			.hasMessage("The Free plan allows 1 court. Upgrade to add more");
		assertThatThrownBy(() -> service.checkStaff(this.venueId, 0)).isInstanceOf(ApiException.class)
			.hasMessage("The Free plan allows no staff accounts. Upgrade to add more");
	}

	private SubscriptionService service(boolean enforce) {
		BillingProperties properties = new BillingProperties(enforce, "Pay FutSlot", 7, 7, Map.of());
		return new SubscriptionService(this.plans, this.subscriptions, mock(SubscriptionInvoiceRepository.class),
				mock(VenueRepository.class), mock(VenueAccess.class), properties, mock(ApplicationEventPublisher.class),
				Clock.systemUTC());
	}

}
