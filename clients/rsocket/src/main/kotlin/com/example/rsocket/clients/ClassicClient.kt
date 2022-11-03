package com.example.rsocket.clients

import org.springframework.beans.factory.annotation.Value
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.retrieveMono
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.util.*

@Component
class ClassicClient(@Value("\${spring.rsocket.server.port}") port: String, val builder: RSocketRequester.Builder) : HelloClient {

    val requester = builder.tcp("localhost", port.toInt())

    override fun hello(lang: Optional<String>, name: String): Mono<String> =
            requester
                    .route("hello")
                    .data(HelloRequest(lang.orElse("en"), name))
                    .retrieveMono()


    override fun names(): Mono<String> =
            requester
                    .route("names")
                    .retrieveMono()

}

