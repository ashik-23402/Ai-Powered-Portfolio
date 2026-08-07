package com.ashik.askaboutme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableAsync
@EnableConfigurationProperties
@SpringBootApplication
public class AskaboutmeApplication {

	public static void main(String[] args) {
		SpringApplication.run(AskaboutmeApplication.class, args);
	}

}
