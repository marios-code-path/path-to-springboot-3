package com.example.webflux.clients

import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.service.annotation.GetExchange
import reactor.core.publisher.Mono


interface HelloClient {
    @GetExchange("/hello/{name}")
    fun hello(@PathVariable name: String)
            : Mono<Greeting>
}

data class Greeting(val greeting: String)