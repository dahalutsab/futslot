package com.futslot;

import org.springframework.boot.SpringApplication;

public class TestFutslotApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(FutslotApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
