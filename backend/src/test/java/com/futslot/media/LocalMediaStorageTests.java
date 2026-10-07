package com.futslot.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalMediaStorageTests {

	@TempDir
	Path root;

	@Test
	void storesReadsAndDeletesUnderItsRootOnly() throws Exception {
		LocalMediaStorage storage = new LocalMediaStorage(new MediaProperties(this.root.resolve("media")));
		Files.writeString(this.root.resolve("secret.txt"), "keep out");

		String url = storage.store("venues/v1/a.png", new byte[] { 1, 2, 3 });
		assertThat(url).isEqualTo("/media/venues/v1/a.png");
		assertThat(storage.keyOf(url)).contains("venues/v1/a.png");
		assertThat(storage.read("venues/v1/a.png")).hasValueSatisfying(bytes -> assertThat(bytes).containsExactly(1, 2, 3));

		assertThat(storage.read("../secret.txt")).isEmpty();
		assertThatThrownBy(() -> storage.store("../escape.png", new byte[] { 1 })).isInstanceOf(IllegalArgumentException.class);
		assertThat(this.root.resolve("escape.png")).doesNotExist();

		storage.delete("venues/v1/a.png");
		assertThat(storage.read("venues/v1/a.png")).isEmpty();
	}

	@Test
	void imagesAreRecognisedByTheirBytes() {
		assertThat(ImageType.detect(new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0 })).contains(ImageType.JPEG);
		assertThat(ImageType.detect("RIFF0000WEBPVP8 ".getBytes())).contains(ImageType.WEBP);
		assertThat(ImageType.detect("<svg onload=alert(1)>".getBytes())).isEmpty();
	}

}
