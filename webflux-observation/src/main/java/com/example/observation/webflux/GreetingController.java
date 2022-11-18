package com.example.observation.webflux;


import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static com.example.observation.webflux.ReactiveLogUtils.logOnNext;

@Slf4j
@RestController
class GreetingController {
    private final GreetingService service;

    GreetingController(GreetingService service) {
        this.service = service;
    }

    @GetMapping("/hello/{name}")
    public Mono<Greeting> greeting(@PathVariable("name") String name, ServerWebExchange exchange) {
        log.info("A Request is made");
        return service
                .greeting(name);
    }
}
