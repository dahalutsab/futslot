package com.futslot.billing;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface SubscriptionInvoiceRepository extends JpaRepository<SubscriptionInvoice, UUID> {

	boolean existsByInvoiceNumber(String invoiceNumber);

	boolean existsBySubscriptionIdAndStatus(UUID subscriptionId, InvoiceStatus status);

	@EntityGraph(attributePaths = { "plan", "subscription" })
	@Query("select i from SubscriptionInvoice i where i.subscription.venueId = :venueId order by i.createdAt desc")
	List<SubscriptionInvoice> findForVenue(UUID venueId);

	List<SubscriptionInvoice> findBySubscriptionIdAndStatusIn(UUID subscriptionId, Collection<InvoiceStatus> statuses);

	@Query("""
			select i from SubscriptionInvoice i
			where i.subscription.id = :subscriptionId and i.periodStart = :periodStart
			  and i.status <> com.futslot.billing.InvoiceStatus.VOID""")
	Optional<SubscriptionInvoice> findForPeriod(UUID subscriptionId, Instant periodStart);

	@EntityGraph(attributePaths = { "plan", "subscription" })
	List<SubscriptionInvoice> findByStatusInOrderByDueOn(Collection<InvoiceStatus> statuses);

	@Query("""
			select i from SubscriptionInvoice i
			where i.status = com.futslot.billing.InvoiceStatus.DUE and i.dueOn < :today""")
	List<SubscriptionInvoice> findOverdue(LocalDate today);

}
