package com.example.observation.reactive;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.annotation.NewSpan;
import io.micrometer.tracing.handler.TracingObservationHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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

    @Scheduled(fixedDelay = 1000, initialDelay = 2000)
    public void logGreetings() {

        service.greeting("Mario 12345")
        .tap(Micrometer.observation(registry))
//        .tag("service-king", "greeting")
//        .tag("service-source", "scheduled")
//        .name("greeting")
        .doOnEach(logOnNext(msg -> log.info("msg: " + msg)))
        .subscribe()

        ;
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
                .tap(Micrometer.observation(registry))
                .name("hello.server.reactive")
                .delayElement(Duration.ofMillis(250))
                .doOnEach(logOnNext(msg -> log.info("msg")));
    }
}

@Service
@Slf4j
class GreetingService {
    private final LatencySupplier latency = new LatencySupplier();

    @Autowired
    Tracer tracer;

    @NewSpan("local-greeting")
    public Mono<Greeting> greeting(String name) {


        Span span = tracer.spanBuilder()
                .name("GreetingService")
                .kind(Span.Kind.SERVER)
                .tag("GreetingType", "HELLO")
                .start();
        Long lat = latency.get();
        return Mono
                .just(new Greeting(name))
                .tag("traceID", span.context().traceId())
                .delayElement(Duration.ofMillis(lat))
                .doOnNext(g -> log.info(String.format("Latency: %d", lat)))
                .doOnEach(logOnNext(c -> log.info("message is: " + c + " " +span.context().traceId() )))
                .doFinally(sig -> span.end())
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