package com.example.observation.reactive;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.ServiceLevelObjectiveBoundary;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.reactive.ServerHttpObservationFilter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.observability.micrometer.Micrometer;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static com.example.observation.reactive.ReactiveLogUtils.logOnNext;
import static java.lang.Math.*;

@Slf4j
@EnableScheduling
@SpringBootApplication
public class ReactiveObservationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReactiveObservationApplication.class, args);
    }

    @Autowired
    GreetingService service;

    @Autowired
    ObservationRegistry registry;

    @Autowired
    MeterRegistry meterRegistry;

    //@Scheduled(fixedDelay = 500)
    public void logGreetings() {

        service.greeting("Mario 12345")
                .name("server.job")
                .tag("job.name", "greeting")
                .tag("span.kind", "server")
                .tap(Micrometer.observation(registry))
                .name("server.job")
                .subscribe()
        ;
    }

    //Base Observation Usage
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

@Slf4j
@RestController
class GreetingController {
    private final GreetingService service;
    private final ObservationRegistry registry;

    GreetingController(GreetingService service, ObservationRegistry registry) {
        this.service = service;
        this.registry = registry;
    }

    @GetMapping("/hello/{name}")
    public Mono<Greeting> greeting(@PathVariable("name") String name, ServerWebExchange exchange) {
        log.info("A Request is made");
        var c = ServerHttpObservationFilter.findObservationContext(exchange)
                .get();

        return service
                .greeting(name)
                .doOnEach(logOnNext(msg -> log.info("msg")));
    }
}

@Service
@Slf4j
class GreetingService {
    private final LatencySupplier latency = new LatencySupplier();

    @Autowired
    ObservationRegistry registry;

    public Mono<Greeting> greeting(String name) {
        Long lat = latency.get();
        return Mono
                .just(new Greeting(name))
                .tag("latency", lat.toString())
                .delayElement(Duration.ofMillis(lat))
                .doOnEach(logOnNext(c -> log.info("greeting: " + c)))
                ;
    }
}

class LatencySupplier implements Supplier<Long> {
    AtomicInteger aInt = new AtomicInteger(0);

    @Override
    public Long get() {
        return 200 + Double.valueOf(abs(floor((sin(Math.PI / 200 * aInt.addAndGet(1))) * 250))).longValue();
    }
}

record Greeting(String name) {
}