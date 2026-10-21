package com.futslot.site;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** The site's wording: every message in both languages, with the same values, and apostrophes MessageFormat keeps. */
class SiteMessagesTests {

	private static final Pattern VALUE = Pattern.compile("\\{(\\d)}");

	@Test
	void everyMessageIsInBothLanguagesWithTheSameValues() throws IOException {
		Properties english = load("i18n/sites.properties");
		Properties nepali = load("i18n/sites_ne.properties");
		assertThat(nepali.stringPropertyNames()).containsExactlyInAnyOrderElementsOf(english.stringPropertyNames());
		for (String key : english.stringPropertyNames()) {
			assertThat(values(nepali.getProperty(key))).as(key).isEqualTo(values(english.getProperty(key)));
		}
	}

	@Test
	void apostrophesAreWrittenForMessageFormatOnlyWhereItsUsed() throws IOException {
		for (String file : new String[] { "i18n/sites.properties", "i18n/sites_ne.properties" }) {
			Properties messages = load(file);
			for (String key : messages.stringPropertyNames()) {
				String message = messages.getProperty(key);
				String single = message.replace("''", "");
				if (VALUE.matcher(message).find()) {
					assertThat(single).as(file + " " + key + " needs '' for an apostrophe").doesNotContain("'");
				}
				else {
					assertThat(message).as(file + " " + key + " is shown as written, so needs a single '").doesNotContain("''");
				}
			}
		}
	}

	private static Set<String> values(String message) {
		Set<String> found = new TreeSet<>();
		Matcher matcher = VALUE.matcher(message);
		while (matcher.find()) {
			found.add(matcher.group());
		}
		return found;
	}

	private static Properties load(String path) throws IOException {
		Properties properties = new Properties();
		try (InputStreamReader reader = new InputStreamReader(new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8)) {
			properties.load(reader);
		}
		return properties;
	}

}
