package com.futslot.media;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Files on Cloudinary, for servers with no lasting disk (Render's free plan wipes its files whenever it sleeps).
 * {@code url} is the API environment variable from Cloudinary's dashboard, cloudinary://key:secret@cloud. Cloudinary
 * serves the files itself, so {@link #store} returns its full address and nothing is read back through FutSlot.
 */
class CloudinaryMediaStorage implements MediaStorage {

	private static final Pattern VERSION = Pattern.compile("^v\\d+/");

	private static final ParameterizedTypeReference<Map<String, Object>> JSON = new ParameterizedTypeReference<>() {
	};

	private final RestClient http;

	private final String apiKey;

	private final String apiSecret;

	private final String folder;

	/** Where the cloud's images are served: https://res.cloudinary.com/cloud/image/upload/ */
	private final String delivery;

	private final Clock clock;

	CloudinaryMediaStorage(String url, String folder, RestClient.Builder http, Clock clock) {
		URI uri = URI.create(url.trim());
		String user = uri.getUserInfo();
		if (!"cloudinary".equals(uri.getScheme()) || user == null || !user.contains(":") || uri.getHost() == null) {
			throw new IllegalArgumentException("futslot.media.cloudinary-url should look like cloudinary://key:secret@cloud");
		}
		this.apiKey = user.substring(0, user.indexOf(':'));
		this.apiSecret = user.substring(user.indexOf(':') + 1);
		this.folder = folder;
		this.delivery = "https://res.cloudinary.com/" + uri.getHost() + "/image/upload/";
		this.http = http.baseUrl("https://api.cloudinary.com/v1_1/" + uri.getHost() + "/image").build();
		this.clock = clock;
	}

	@Override
	public String store(String key, byte[] content) {
		MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
		signed(Map.of("public_id", publicId(key))).forEach(form::add);
		form.add("file", new ByteArrayResource(content) {
			@Override
			public String getFilename() {
				return key.substring(key.lastIndexOf('/') + 1);
			}
		});
		Map<String, Object> uploaded = this.http.post()
			.uri("/upload")
			.contentType(MediaType.MULTIPART_FORM_DATA)
			.body(form)
			.retrieve()
			.body(JSON);
		if (uploaded == null || !(uploaded.get("secure_url") instanceof String address)) {
			throw new IllegalStateException("Cloudinary didn't say where " + key + " is");
		}
		return address;
	}

	@Override
	public void delete(String key) {
		this.http.post()
			.uri("/destroy")
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.body(MultiValueMap.fromSingleValue(signed(Map.of("public_id", publicId(key)))))
			.retrieve()
			.toBodilessEntity();
	}

	/** Cloudinary serves the files; nothing is served from /media/. */
	@Override
	public Optional<byte[]> read(String key) {
		return Optional.empty();
	}

	/** https://res.cloudinary.com/cloud/image/upload/v123/futslot/venues/v1/a.jpg is venues/v1/a.jpg */
	@Override
	public Optional<String> keyOf(String url) {
		if (url == null || !url.startsWith(this.delivery)) {
			return Optional.empty();
		}
		String path = VERSION.matcher(url.substring(this.delivery.length())).replaceFirst("");
		return path.startsWith(this.folder + "/") ? Optional.of(path.substring(this.folder.length() + 1)) : Optional.empty();
	}

	/** The key without its extension, in FutSlot's folder: Cloudinary adds the extension itself. */
	private String publicId(String key) {
		int dot = key.lastIndexOf('.');
		return this.folder + "/" + ((dot > key.lastIndexOf('/')) ? key.substring(0, dot) : key);
	}

	/** The parameters plus a timestamp, the API key and their signature (Cloudinary's signed upload). */
	private Map<String, String> signed(Map<String, String> parameters) {
		Map<String, String> signed = new TreeMap<>(parameters);
		signed.put("timestamp", Long.toString(this.clock.instant().getEpochSecond()));
		String text = signed.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining("&"));
		Map<String, String> body = new LinkedHashMap<>(signed);
		body.put("api_key", this.apiKey);
		body.put("signature", sha1(text + this.apiSecret));
		return body;
	}

	private static String sha1(String text) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(text.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
