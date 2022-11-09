package com.example.webflux.clients

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.service.annotation.GetExchange
import reactor.core.publisher.Mono


interface SalutationClient {
    @GetExchange("/hello/{name}", accept = [MediaType.APPLICATION_JSON_VALUE])
    fun hello(@PathVariable name: String): Mono<Salutation>

    @GetExchange("/hello/{name}", accept = [MediaType.APPLICATION_JSON_VALUE])
    fun entityHello(@PathVariable name: String): Mono<ResponseEntity<Salutation>>
}

data class Salutation(val greeting: String)