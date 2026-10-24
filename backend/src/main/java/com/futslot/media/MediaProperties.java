package com.futslot.media;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * {@code dir} should be a mounted volume in production so uploads survive redeploys. A {@code cloudinaryUrl}
 * (cloudinary://key:secret@cloud) keeps them on Cloudinary instead, in {@code cloudinaryFolder}, for servers with no
 * lasting disk.
 */
@ConfigurationProperties("futslot.media")
record MediaProperties(@DefaultValue("media") Path dir, String cloudinaryUrl, @DefaultValue("futslot") String cloudinaryFolder) {

}
