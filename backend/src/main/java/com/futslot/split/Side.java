package com.futslot.split;

public enum Side {

	A, B;

	Side other() {
		return (this == A) ? B : A;
	}

}
