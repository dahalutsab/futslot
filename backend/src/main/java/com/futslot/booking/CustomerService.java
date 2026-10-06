package com.futslot.booking;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.futslot.common.ApiException;
import com.futslot.common.PageResponse;
import com.futslot.venue.VenueAccess;
import com.futslot.venue.VenuePermission;

/** The venue's customer book, built automatically from bookings. */
@Service
class CustomerService {

	private static final int RECENT_BOOKINGS = 20;

	private final VenueCustomerRepository customers;

	private final BookingRepository bookings;

	private final VenueAccess access;

	CustomerService(VenueCustomerRepository customers, BookingRepository bookings, VenueAccess access) {
		this.customers = customers;
		this.bookings = bookings;
		this.access = access;
	}

	@Transactional(readOnly = true)
	PageResponse<CustomerSummaryResponse> search(UUID userId, UUID venueId, String query, CustomerSort sort, int page,
			int size) {
		this.access.require(userId, venueId, VenuePermission.VIEW_CUSTOMERS);
		String pattern = null;
		if (StringUtils.hasText(query)) {
			String term = query.strip().toLowerCase();
			// "981-234" should find "+977981234..."; names keep their spaces
			if (term.matches("[+\\d\\s()-]+")) {
				term = term.replaceAll("[^\\d]", "");
			}
			pattern = "%" + term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
		}
		return PageResponse.of(this.customers.search(venueId, pattern, PageRequest.of(page, size, sort.sort)),
				CustomerSummaryResponse::from);
	}

	@Transactional(readOnly = true)
	CustomerDetailResponse get(UUID userId, UUID venueId, UUID customerId) {
		this.access.require(userId, venueId, VenuePermission.VIEW_CUSTOMERS);
		return detail(customer(venueId, customerId));
	}

	@Transactional
	CustomerDetailResponse update(UUID userId, UUID venueId, UUID customerId, CustomerUpdateRequest request) {
		this.access.require(userId, venueId, VenuePermission.VIEW_CUSTOMERS);
		VenueCustomer customer = customer(venueId, customerId);
		customer.update((request.name() != null) ? request.name().strip() : null, request.notes());
		return detail(customer);
	}

	private CustomerDetailResponse detail(VenueCustomer customer) {
		Map<BookingStatus, BookingRepository.StatusTotal> totals = this.bookings.totalsByStatus(customer.getId())
			.stream()
			.collect(Collectors.toMap(BookingRepository.StatusTotal::getStatus, total -> total));
		long all = totals.values().stream().mapToLong(BookingRepository.StatusTotal::getBookings).sum();
		CustomerDetailResponse.Stats stats = new CustomerDetailResponse.Stats(all,
				count(totals, BookingStatus.CONFIRMED) + count(totals, BookingStatus.PENDING_PAYMENT),
				count(totals, BookingStatus.COMPLETED), count(totals, BookingStatus.CANCELLED),
				count(totals, BookingStatus.NO_SHOW),
				totals.containsKey(BookingStatus.COMPLETED) ? totals.get(BookingStatus.COMPLETED).getAmount()
						: BigDecimal.ZERO);
		List<BookingResponse> recent = this.bookings
			.findByCustomerIdOrderByStartsAtDesc(customer.getId(), PageRequest.of(0, RECENT_BOOKINGS))
			.stream()
			.map(BookingResponse::from)
			.toList();
		return new CustomerDetailResponse(customer.getId(), customer.getName(), customer.getPhone(),
				customer.getUserId() != null, customer.getNotes(), customer.getCreatedAt(), customer.getLastBookingAt(),
				stats, recent);
	}

	private static long count(Map<BookingStatus, BookingRepository.StatusTotal> totals, BookingStatus status) {
		return totals.containsKey(status) ? totals.get(status).getBookings() : 0;
	}

	private VenueCustomer customer(UUID venueId, UUID customerId) {
		return this.customers.findByIdAndVenueId(customerId, venueId)
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer not found"));
	}

	enum CustomerSort {

		RECENT(Sort.by(Sort.Order.desc("lastBookingAt").nullsLast(), Sort.Order.asc("name"))),

		FREQUENT(Sort.by(Sort.Order.desc("totalBookings"), Sort.Order.asc("name"))),

		NAME(Sort.by("name"));

		private final Sort sort;

		CustomerSort(Sort sort) {
			this.sort = sort;
		}

	}

}
