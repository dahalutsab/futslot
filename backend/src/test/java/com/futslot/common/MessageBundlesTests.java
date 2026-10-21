package com.futslot.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * The wording in i18n/: every message in both languages with the same values, apostrophes MessageFormat keeps,
 * and Nepali error messages only for codes that exist.
 */
class MessageBundlesTests {

	private static final List<String> BUNDLES = List.of("sites", "notices");

	private static final Pattern VALUE = Pattern.compile("\\{(\\d)}");

	@Test
	void everyMessageIsInBothLanguagesWithTheSameValues() throws IOException {
		for (String bundle : BUNDLES) {
			Properties english = load("i18n/" + bundle + ".properties");
			Properties nepali = load("i18n/" + bundle + "_ne.properties");
			assertThat(nepali.stringPropertyNames()).as(bundle).containsExactlyInAnyOrderElementsOf(english.stringPropertyNames());
			for (String key : english.stringPropertyNames()) {
				assertThat(values(nepali.getProperty(key))).as(bundle + " " + key).isEqualTo(values(english.getProperty(key)));
			}
		}
	}

	@Test
	void apostrophesAreWrittenForMessageFormatOnlyWhereItsUsed() throws IOException {
		for (String file : List.of("sites", "sites_ne", "notices", "notices_ne", "errors_ne")) {
			Properties messages = load("i18n/" + file + ".properties");
			for (String key : messages.stringPropertyNames()) {
				String message = messages.getProperty(key);
				if (VALUE.matcher(message).find()) {
					assertThat(message.replace("''", "")).as(file + " " + key + " needs '' for an apostrophe").doesNotContain("'");
				}
				else {
					assertThat(message).as(file + " " + key + " is shown as written, so needs a single '").doesNotContain("''");
				}
			}
		}
	}

	@Test
	void nepaliErrorsAreForCodesTheApiHas() throws IOException {
		String source;
		try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
			StringBuilder all = new StringBuilder();
			for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
				all.append(Files.readString(file));
			}
			source = all.toString();
		}
		for (String key : load("i18n/errors_ne.properties").stringPropertyNames()) {
			String[] parts = key.split("\\.");
			assertThat(parts[0]).as(key).isEqualTo("error");
			assertThat(source).as(key).contains("\"" + parts[1] + "\"");
			if (parts.length > 2) {
				assertThat(source).as(key).contains(".variant(\"" + parts[2] + "\")");
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
