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

    @RequestMapping("hello/{name}")
    fun hello(@RequestParam("lang", required = false) lang: Optional<String>, @PathVariable name: String): Mono<String> =
            Mono.create { sink ->
                names.add(name)
                val hello = helloLangs[lang.orElse("jp")]
                sink.success("$hello $name")
            }

    @RequestMapping("names")
    fun names(): Mono<String> = Mono.just(names.stream().collect(Collectors.joining()))
}