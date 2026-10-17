package com.futslot.payment;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface PaymentRepository extends JpaRepository<Payment, UUID> {

	boolean existsByMethodAndTransactionId(PaymentMethod method, String transactionId);

	boolean existsByBookingIdAndStatus(UUID bookingId, TransactionStatus status);

	boolean existsByEntryIdAndStatus(UUID registrationId, TransactionStatus status);

	boolean existsByInvoiceIdAndStatus(UUID invoiceId, TransactionStatus status);

	boolean existsByListingIdAndStatus(UUID listingId, TransactionStatus status);

	boolean existsByPurposeAndPayerUserIdAndStatus(PaymentPurpose purpose, UUID payerUserId, TransactionStatus status);

	List<Payment> findByStatusAndPurposeInOrderByCreatedAt(TransactionStatus status, Collection<PaymentPurpose> purposes);

	List<Payment> findByBookingIdOrderByCreatedAt(UUID bookingId);

	List<Payment> findByBookingIdAndStatusIn(UUID bookingId, Collection<TransactionStatus> statuses);

	Optional<Payment> findByIdAndVenueId(UUID id, UUID venueId);

	List<Payment> findByVenueIdAndStatusOrderByCreatedAt(UUID venueId, TransactionStatus status);

	@Query("select coalesce(sum(r.amount), 0) from Refund r where r.payment.id = :paymentId and r.status <> com.futslot.payment.RefundStatus.REJECTED")
	BigDecimal refundedOrOwed(UUID paymentId);

}
