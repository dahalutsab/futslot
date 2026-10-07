package com.futslot;

import org.springframework.boot.SpringApplication;

/** Runs the API locally against a throwaway database, with the {@code dev} profile (see application-dev.properties). */
public class TestFutslotApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(FutslotApiApplication::main)
			.with(TestcontainersConfiguration.class)
			.withAdditionalProfiles("dev")
			.run(args);
	}

}
