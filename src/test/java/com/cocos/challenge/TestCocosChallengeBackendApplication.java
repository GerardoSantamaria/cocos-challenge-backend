package com.cocos.challenge;

import org.springframework.boot.SpringApplication;

public class TestCocosChallengeBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(CocosChallengeBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
