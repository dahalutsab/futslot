package com.futslot.venue;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.futslot.common.CurrentUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/owner/venues/{venueId}/photos")
class PhotoController {

	private final PhotoService photoService;

	PhotoController(PhotoService photoService) {
		this.photoService = photoService;
	}

	@GetMapping
	List<PhotoResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId) {
		return this.photoService.list(CurrentUser.id(jwt), venueId);
	}

	/** Multipart form: {@code file} (JPEG, PNG or WebP, up to 5 MB) and an optional {@code caption}. */
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	PhotoResponse upload(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@RequestPart("file") MultipartFile file, @RequestParam(required = false) String caption) {
		return this.photoService.upload(CurrentUser.id(jwt), venueId, file, caption);
	}

	@PutMapping("/{photoId}")
	List<PhotoResponse> update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId, @PathVariable UUID photoId,
			@Valid @RequestBody PhotoUpdateRequest request) {
		return this.photoService.update(CurrentUser.id(jwt), venueId, photoId, request);
	}

	@DeleteMapping("/{photoId}")
	List<PhotoResponse> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId, @PathVariable UUID photoId) {
		return this.photoService.delete(CurrentUser.id(jwt), venueId, photoId);
	}

}
