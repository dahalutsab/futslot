package com.futslot.media;

import java.time.Duration;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.servlet.http.HttpServletRequest;

/** Public: uploaded images. File names are never reused, so they can be cached for a long time. */
@RestController
class MediaController {

	private final MediaStorage storage;

	MediaController(MediaStorage storage) {
		this.storage = storage;
	}

	@GetMapping("/media/**")
	@SecurityRequirements
	ResponseEntity<byte[]> get(HttpServletRequest request) {
		String key = request.getRequestURI().substring(request.getContextPath().length() + LocalMediaStorage.URL_PREFIX.length());
		ImageType type = ImageType.byExtension(key).orElse(null);
		if (type == null) {
			return ResponseEntity.notFound().build();
		}
		return this.storage.read(key)
			.map(bytes -> ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(type.contentType()))
				.cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
				.header("X-Content-Type-Options", "nosniff")
				.body(bytes))
			.orElseGet(() -> ResponseEntity.notFound().build());
	}

}
