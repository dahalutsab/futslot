package com.futslot.config;

import java.util.List;
import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import com.futslot.common.Languages;

import jakarta.servlet.http.HttpServletRequest;

@Configuration
public class LocaleConfig {

	/** The language a page chose for itself, otherwise English or Nepali from Accept-Language, otherwise English. */
	@Bean
	LocaleResolver localeResolver() {
		AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver() {

			@Override
			public Locale resolveLocale(HttpServletRequest request) {
				return (request.getAttribute(Languages.CHOSEN) instanceof Locale chosen) ? chosen : super.resolveLocale(request);
			}

		};
		resolver.setSupportedLocales(List.of(Languages.ENGLISH, Languages.NEPALI));
		resolver.setDefaultLocale(Languages.ENGLISH);
		return resolver;
	}

}
