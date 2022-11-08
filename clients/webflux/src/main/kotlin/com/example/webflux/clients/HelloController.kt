package com.example.webflux.clients

import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*
import java.util.stream.Collectors

@RestController
class HelloController(val svc: HelloService) {
    @RequestMapping("hello/{name}")
    fun hello(@RequestParam("lang", required = false) lang: Optional<String>,
              @PathVariable name: String): Mono<String> =
            Mono.create { sink ->
                val hello = svc.sayHello(name, lang.orElse("en"))
                sink.success(hello)
            }
}