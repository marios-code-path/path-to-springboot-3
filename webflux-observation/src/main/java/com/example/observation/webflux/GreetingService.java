package com.example.observation.webflux;

import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.observability.micrometer.Micrometer;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static java.lang.Math.*;

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
                .delayElement(Duration.ofMillis(lat))
                .name("greeting.call")
                .tag("latency", lat.toString())
                .tap(Micrometer.observation(registry))
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