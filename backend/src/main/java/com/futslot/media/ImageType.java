package com.futslot.media;

import java.util.Optional;

/** Images we accept, recognised by their first bytes rather than by what the upload claims to be. */
public enum ImageType {

	JPEG("jpg", "image/jpeg"), PNG("png", "image/png"), WEBP("webp", "image/webp");

	private final String extension;

	private final String contentType;

	ImageType(String extension, String contentType) {
		this.extension = extension;
		this.contentType = contentType;
	}

	public String extension() {
		return this.extension;
	}

	public String contentType() {
		return this.contentType;
	}

	public static Optional<ImageType> detect(byte[] bytes) {
		if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
			return Optional.of(JPEG);
		}
		if (bytes.length >= 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
			return Optional.of(PNG);
		}
		if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
				&& bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
			return Optional.of(WEBP);
		}
		return Optional.empty();
	}

	static Optional<ImageType> byExtension(String key) {
		String lower = key.toLowerCase();
		for (ImageType type : values()) {
			if (lower.endsWith("." + type.extension)) {
				return Optional.of(type);
			}
		}
		return Optional.empty();
	}

}
