package com.futslot.media;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/** Files under {@code futslot.media.dir}. Keys can't climb out of it. */
class LocalMediaStorage implements MediaStorage {

	static final String URL_PREFIX = "/media/";

	private final Path root;

	LocalMediaStorage(MediaProperties properties) {
		this.root = properties.dir().toAbsolutePath().normalize();
	}

	@Override
	public String store(String key, byte[] content) {
		Path file = resolve(key);
		try {
			Files.createDirectories(file.getParent());
			Files.write(file, content);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Could not store " + key, ex);
		}
		return URL_PREFIX + key;
	}

	@Override
	public void delete(String key) {
		try {
			Files.deleteIfExists(resolve(key));
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Could not delete " + key, ex);
		}
	}

	@Override
	public Optional<byte[]> read(String key) {
		Path file;
		try {
			file = resolve(key);
		}
		catch (IllegalArgumentException ex) {
			return Optional.empty();
		}
		if (!Files.isRegularFile(file)) {
			return Optional.empty();
		}
		try {
			return Optional.of(Files.readAllBytes(file));
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Could not read " + key, ex);
		}
	}

	@Override
	public Optional<String> keyOf(String url) {
		return (url != null && url.startsWith(URL_PREFIX)) ? Optional.of(url.substring(URL_PREFIX.length()))
				: Optional.empty();
	}

	private Path resolve(String key) {
		Path file = this.root.resolve(key).normalize();
		if (!file.startsWith(this.root) || file.equals(this.root)) {
			throw new IllegalArgumentException("Bad media key " + key);
		}
		return file;
	}

}
