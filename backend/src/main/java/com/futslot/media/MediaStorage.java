package com.futslot.media;

import java.util.Optional;

/**
 * Where uploaded files live. {@code key} is a relative path such as {@code venues/<id>/<file>.jpg}; files
 * are served at {@code /media/<key>}, or by Cloudinary (see {@link MediaConfig}).
 */
public interface MediaStorage {

	/** Saves the file and returns the URL path it's served at. */
	String store(String key, byte[] content);

	void delete(String key);

	Optional<byte[]> read(String key);

	/** The key behind a URL returned by {@link #store}, if it is one of ours. */
	Optional<String> keyOf(String url);

}
