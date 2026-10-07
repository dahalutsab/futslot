package com.futslot.review;

import org.springframework.data.domain.Sort;

enum ReviewSort {

	NEWEST(Sort.by(Sort.Order.desc("createdAt"))),

	HIGHEST(Sort.by(Sort.Order.desc("rating"), Sort.Order.desc("createdAt"))),

	LOWEST(Sort.by(Sort.Order.asc("rating"), Sort.Order.desc("createdAt")));

	private final Sort sort;

	ReviewSort(Sort sort) {
		this.sort = sort;
	}

	Sort sort() {
		return this.sort;
	}

}
