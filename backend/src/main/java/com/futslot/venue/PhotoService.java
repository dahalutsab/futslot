package com.futslot.venue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.futslot.common.ApiException;
import com.futslot.media.ImageType;
import com.futslot.media.MediaStorage;

/**
 * Venue photos (MANAGE_VENUE). JPEG, PNG or WebP up to 5 MB, checked by content, at most
 * {@link #MAX_PHOTOS} per venue. The first photo becomes the cover; deleting the cover promotes the next.
 */
@Service
class PhotoService {

	static final int MAX_PHOTOS = 15;

	static final long MAX_BYTES = 5L * 1024 * 1024;

	private final VenuePhotoRepository photos;

	private final VenueAccess access;

	private final MediaStorage storage;

	PhotoService(VenuePhotoRepository photos, VenueAccess access, MediaStorage storage) {
		this.photos = photos;
		this.access = access;
		this.storage = storage;
	}

	@Transactional
	PhotoResponse upload(UUID userId, UUID venueId, MultipartFile file, String caption) {
		this.access.require(userId, venueId, VenuePermission.MANAGE_VENUE);
		if (file == null || file.isEmpty()) {
			throw badRequest("FILE_REQUIRED", "Choose a photo to upload");
		}
		if (file.getSize() > MAX_BYTES) {
			throw badRequest("FILE_TOO_LARGE", "Photos can be up to 5 MB");
		}
		long existing = this.photos.countByVenueId(venueId);
		if (existing >= MAX_PHOTOS) {
			throw new ApiException(HttpStatus.CONFLICT, "TOO_MANY_PHOTOS", "A venue can have up to " + MAX_PHOTOS + " photos");
		}
		byte[] bytes = bytes(file);
		ImageType type = ImageType.detect(bytes)
			.orElseThrow(() -> badRequest("UNSUPPORTED_IMAGE", "Upload a JPEG, PNG or WebP photo"));
		String key = "venues/" + venueId + "/" + UUID.randomUUID() + "." + type.extension();
		String url = this.storage.store(key, bytes);
		// If the database write fails, don't leave the file behind
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCompletion(int status) {
				if (status != STATUS_COMMITTED) {
					PhotoService.this.storage.delete(key);
				}
			}
		});
		VenuePhoto photo = this.photos.save(new VenuePhoto(venueId, url, text(caption), (int) existing, existing == 0));
		return PhotoResponse.from(photo);
	}

	@Transactional(readOnly = true)
	List<PhotoResponse> list(UUID userId, UUID venueId) {
		this.access.require(userId, venueId, null);
		return forVenue(venueId);
	}

	@Transactional
	List<PhotoResponse> update(UUID userId, UUID venueId, UUID photoId, PhotoUpdateRequest request) {
		this.access.require(userId, venueId, VenuePermission.MANAGE_VENUE);
		VenuePhoto photo = photo(venueId, photoId);
		if (request.caption() != null) {
			photo.setCaption(text(request.caption()));
		}
		if (request.sortOrder() != null) {
			photo.setSortOrder(request.sortOrder());
		}
		if (Boolean.TRUE.equals(request.cover()) && !photo.isCover()) {
			this.photos.findByVenueIdAndIsCoverTrue(venueId).ifPresent(cover -> cover.setCover(false));
			this.photos.flush();
			photo.setCover(true);
		}
		return forVenue(venueId);
	}

	@Transactional
	List<PhotoResponse> delete(UUID userId, UUID venueId, UUID photoId) {
		this.access.require(userId, venueId, VenuePermission.MANAGE_VENUE);
		VenuePhoto photo = photo(venueId, photoId);
		this.photos.delete(photo);
		this.photos.flush();
		if (photo.isCover()) {
			this.photos.findByVenueIdOrderByIsCoverDescSortOrderAscCreatedAtAsc(venueId)
				.stream()
				.findFirst()
				.ifPresent(next -> next.setCover(true));
		}
		// The file goes once the row is gone for good
		this.storage.keyOf(photo.getUrl()).ifPresent(key -> TransactionSynchronizationManager
			.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					PhotoService.this.storage.delete(key);
				}
			}));
		return forVenue(venueId);
	}

	List<PhotoResponse> forVenue(UUID venueId) {
		return this.photos.findByVenueIdOrderByIsCoverDescSortOrderAscCreatedAtAsc(venueId)
			.stream()
			.map(PhotoResponse::from)
			.toList();
	}

	private VenuePhoto photo(UUID venueId, UUID photoId) {
		return this.photos.findByIdAndVenueId(photoId, venueId)
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PHOTO_NOT_FOUND", "Photo not found"));
	}

	private static byte[] bytes(MultipartFile file) {
		try {
			return file.getBytes();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static String text(String value) {
		return (value != null && !value.isBlank()) ? value.strip() : null;
	}

	private static ApiException badRequest(String code, String message) {
		return new ApiException(HttpStatus.BAD_REQUEST, code, message);
	}

}
