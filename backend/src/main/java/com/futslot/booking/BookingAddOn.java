package com.futslot.booking;

import java.math.BigDecimal;

import com.futslot.venue.AddOnItem;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/** An extra on a booking, at the price it had when added. */
@Embeddable
public class BookingAddOn {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "add_on_item_id", nullable = false)
	private AddOnItem item;

	@Column(nullable = false)
	private int quantity;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal unitPrice;

	protected BookingAddOn() {
	}

	BookingAddOn(AddOnItem item, int quantity) {
		this.item = item;
		this.quantity = quantity;
		this.unitPrice = item.getPrice();
	}

	BookingAddOn atPrice(BigDecimal price) {
		this.unitPrice = price;
		return this;
	}

	BigDecimal total() {
		return this.unitPrice.multiply(BigDecimal.valueOf(this.quantity));
	}

	public AddOnItem getItem() {
		return this.item;
	}

	public int getQuantity() {
		return this.quantity;
	}

	public BigDecimal getUnitPrice() {
		return this.unitPrice;
	}

}
