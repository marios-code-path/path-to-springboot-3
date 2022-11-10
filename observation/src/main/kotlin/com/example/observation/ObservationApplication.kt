package com.example.observation

import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry
import io.micrometer.observation.annotation.Observed
import io.micrometer.observation.aop.ObservedAspect
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sin

@SpringBootApplication
class ObservationApplication {

    @Bean
    fun observationRegistry(): ObservationRegistry = ObservationRegistry.create()

    @Bean
    fun observedAspect(observationRegistry: ObservationRegistry): ObservedAspect =
            ObservedAspect(observationRegistry)
}

// TODO AOT for kotlin wont work without this.
// TODO https://github.com/spring-projects/spring-boot/issues/32918
fun main(args: Array<String>) {
    runApplication<ObservationApplication>(*args)
}

@RestController
class MyRestController(val svc: HelloService, val registry: ObservationRegistry) {

    val logger: Logger = LoggerFactory.getLogger(MyRestController::class.java)

    @GetMapping("/hello/{name}")
    fun hello(@PathVariable("name") name: String): Greeting {
        logger.info("Received request for salutation")
        return svc.getHello(name)
    }

    @GetMapping("/ping")
    fun pingPong(): String {
        var pong: String = ""
        Observation.createNotStarted("ping.pong", registry)
                .contextualName("get-ping-pong")
                .lowCardinalityKeyValue("pingType", "pong")
                .observe {
                    pong = svc.pingPong()
                }
        return pong;
    }
}

@Service
class HelloService {
    val log: Logger = LoggerFactory.getLogger(HelloService::class.java)

    val aInt: AtomicInteger = AtomicInteger(0)

    @Observed(name = "greeting",
            contextualName = "say-hi",
            lowCardinalityKeyValues = ["GreetingType", "Salutation"])
    fun getHello(name: String): Greeting {

        if (StringUtils.hasText(name) && name[0].isDigit()) {
            throw IllegalArgumentException("Invalid name format.")
        }
        // Equation Plucked from:
        // https://cs.smu.ca/~porter/csc/465/code/eckel/c13/SineWave.java2html
        val latency: Long = abs(floor((sin(Math.PI / 200 * aInt.addAndGet(1))) * 250).toLong())

        log.info("Waiting for $latency ms.")

        Thread.sleep(latency)

        return Greeting("HELLO THERE, $name at ${System.currentTimeMillis()} delayed for $latency ms.")
    }

    fun pingPong(): String {
        return "Pong"
    }
}

data class Greeting(val greeting: String)