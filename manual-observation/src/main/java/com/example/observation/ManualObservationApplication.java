package com.example.observation;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.ObservationTextPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Slf4j
@SpringBootApplication
public class ManualObservationApplication {

	public static void main(String[] args) {
		SpringApplication.run(ManualObservationApplication.class, args);
	}

	@Autowired
	ObservationRegistry registry;

	@Bean
	public ApplicationListener<ApplicationStartedEvent> doOnStart() {
		return event -> {
			generateString();
		};
	}

	public void generateString() {
		String something = Observation
				.createNotStarted("server.job", registry)
				.lowCardinalityKeyValue("jobType", "string")
				.observe(() -> {
					log.info("Generating a String...");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
						return "NOTHING";
					}
					return "SOMETHING";
				});

		log.info("Result was: " + something);
	}

}
