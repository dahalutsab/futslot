package com.futslot.media;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/** Uploads go to Cloudinary when it's set up, otherwise to the local disk. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MediaProperties.class)
class MediaConfig {

	@Bean
	MediaStorage mediaStorage(MediaProperties properties, Clock clock) {
		return StringUtils.hasText(properties.cloudinaryUrl())
				? new CloudinaryMediaStorage(properties.cloudinaryUrl(), properties.cloudinaryFolder(), RestClient.builder(), clock)
				: new LocalMediaStorage(properties);
	}

}
