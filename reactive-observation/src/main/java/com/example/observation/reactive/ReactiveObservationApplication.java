package com.example.observation.reactive;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.observability.micrometer.Micrometer;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Math.*;

@SpringBootApplication
public class ReactiveObservationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReactiveObservationApplication.class, args);
    }

}

@Slf4j
@RestController
class GreetingController {
    private final GreetingService service;
    private final ObservationRegistry registry;

    GreetingController(GreetingService service, ObservationRegistry registry) {
        this.service = service;
        this.registry = registry;
    }

    @GetMapping("/greeting/{name}")
    public Mono<Greeting> greeting(@PathVariable("name") String name) {
        log.info("A Request is made");
        return service
                .greeting(name)
                .log()
                .doOnNext(greeting -> {
                    log.info("Received a greeting");
                });
    }
}

@Service
@Slf4j
class GreetingService {
    private final LatencyService latency = new LatencyService();
	private final ObservationRegistry registry;

	GreetingService(ObservationRegistry registry) {
		this.registry = registry;
	}

	public Mono<Greeting> greeting(String name) {
		Long lat = latency.latency();

        return Mono.just(new Greeting(name))
				.tap(Micrometer.observation(registry))
				.tag("greetingType", "REST")
				.name("GreetingService")
                .delayElement(Duration.ofMillis(lat))
				.doOnNext ( g -> log.info(String.format("Latency: %d", lat)))
                ;
    }
}


class LatencyService {
    AtomicInteger aInt = new AtomicInteger(0);

    public Long latency() {
        return 200 + Double.valueOf(abs(floor((sin(Math.PI / 200 * aInt.addAndGet(1))) * 250))).longValue();
    }
}

record Greeting(String name) {
}