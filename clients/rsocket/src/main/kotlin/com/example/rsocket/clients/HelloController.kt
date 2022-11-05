package com.example.rsocket.clients

import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller
import reactor.core.publisher.Mono
import java.util.stream.Collectors

@Controller
class HelloController {
    val helloLangs: Map<String, String> = mapOf(
            Pair("en", "Hello"),
            Pair("fr", "Bonjour"),
            Pair("de", "Guten tag"),
            Pair("it", "Salve"),
            Pair("cn", "nǐn hǎo"),
            Pair("ara", "asalaam alaikum"),
            Pair("jp", "konnichiwa")
    )

    val names: MutableSet<String> = HashSet()

    @MessageMapping("hello")
    fun hello(req: HelloRequest): Mono<String> =
            Mono.create { sink ->
                names.add(req.name)
                val hello = helloLangs[req.lang]
                sink.success("$hello ${req.name}")
            }

    @MessageMapping("names")
    fun names(): Mono<String> = Mono.just(names.stream().collect(Collectors.joining()))
}

data class HelloRequest(val lang: String, val name: String);