package com.futslot.common;

public final class Names {

	private Names() {
	}

	/** How players appear to people who don't know them: "Ram Bahadur Shrestha" -> "Ram S.". */
	public static String shortName(String fullName) {
		String[] parts = fullName.strip().split("\\s+");
		if (parts.length == 1) {
			return parts[0];
		}
		return parts[0] + " " + parts[parts.length - 1].charAt(0) + ".";
	}

}
