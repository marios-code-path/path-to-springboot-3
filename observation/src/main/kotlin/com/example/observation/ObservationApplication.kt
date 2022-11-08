package com.example.observation

import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationHandler
import io.micrometer.observation.ObservationRegistry
import io.micrometer.observation.annotation.Observed
import io.micrometer.observation.aop.ObservedAspect
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.ui.ModelMap
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.WebRequest
import org.springframework.web.context.request.WebRequestInterceptor
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.handler.WebRequestHandlerInterceptorAdapter
import java.lang.Exception
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sin

@SpringBootApplication
class ObservationApplication {

    @Bean
    fun observedAspect(observationRegistry: ObservationRegistry): ObservedAspect =
            ObservedAspect(observationRegistry)
}

// TODO The traces are also not communicated with AOT
// TODO AOT for kotlin wont work without this.
// TODO https://github.com/spring-projects/spring-boot/issues/32918
fun main(args: Array<String>) {
    runApplication<ObservationApplication>(*args)
}

@RestController
class MyRestController(val svc: HelloService) {
    val logger: Logger = LoggerFactory.getLogger(HelloHandler::class.java)

    @GetMapping("/hello/{name}")
    fun hello(@PathVariable("name") name: String): Greeting {

        logger.info("Received request for salutation")
        return svc.getHello(name)
    }
}

@Service
class HelloService {

    val aInt: AtomicInteger = AtomicInteger(0)

    @Observed(name = "greeting",
            contextualName = "say-hi",
            lowCardinalityKeyValues = ["GreetingType", "Salutation"])
    fun getHello(name: String): Greeting {

        if (StringUtils.hasText(name) &&
                name[0].isDigit()) {
            throw IllegalArgumentException("Invalid name format.")
        }
        // Plucked from:
        // https://cs.smu.ca/~porter/csc/465/code/eckel/c13/SineWave.java2html
        val latency: Long = abs(floor((sin(Math.PI/200 * aInt.addAndGet(1) )) * 250 ).toLong())

        Thread.sleep(latency)
        return Greeting("HELLO THERE, $name at ${System.currentTimeMillis()} delayed for $latency ms.")
    }
}


@Component
class HelloHandler : ObservationHandler<Observation.Context> {

    val logger: Logger = LoggerFactory.getLogger(HelloHandler::class.java)

    override fun onStart(context: Observation.Context) {
        logger.info("Started observation of ${context.name}")
    }

    override fun onStop(context: Observation.Context) {
        logger.info("Stopped observation for ${context.name}")
    }

    override fun supportsContext(context: Observation.Context): Boolean = true
}

data class Greeting(val greeting: String)