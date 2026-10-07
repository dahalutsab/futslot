package com.futslot.media;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** {@code dir} should be a mounted volume in production so uploads survive redeploys. */
@ConfigurationProperties("futslot.media")
record MediaProperties(@DefaultValue("media") Path dir) {

}
