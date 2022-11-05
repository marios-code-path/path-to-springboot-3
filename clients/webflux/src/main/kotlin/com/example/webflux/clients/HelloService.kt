package com.example.webflux.clients

import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import java.util.HashSet

@Service
class HelloService {
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

    @Observed(name = "greeting",
            contextualName = "say-hello",
            lowCardinalityKeyValues = ["greeting", "hello"])
    fun sayHello(name: String, lang: String) =
            "${helloLangs[lang]} $name!"
}