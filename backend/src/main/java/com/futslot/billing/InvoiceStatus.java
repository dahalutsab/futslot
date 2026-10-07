package com.futslot.billing;

public enum InvoiceStatus {

	DUE, PAID, OVERDUE, VOID;

	boolean isOpen() {
		return this == DUE || this == OVERDUE;
	}

}
