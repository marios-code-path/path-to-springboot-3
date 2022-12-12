package com.example.observation.webflux;

import io.micrometer.observation.ObservationTextPublisher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class WebfluxObservationApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebfluxObservationApplication.class, args);
	}

	@Bean
	public ObservationTextPublisher printingObservationHandler() {
		return new ObservationTextPublisher();
	}
}
