package com.futslot.venue;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Reference data seeded by migration V2. */
@Entity
@Table(name = "amenities")
public class Amenity {

	@Id
	private String code;

	private String name;

	private String icon;

	protected Amenity() {
	}

	public String getCode() {
		return this.code;
	}

	public String getName() {
		return this.name;
	}

	public String getIcon() {
		return this.icon;
	}

}
