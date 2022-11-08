package com.example.webflux.clients

import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry
import io.micrometer.observation.aop.ObservedAspect
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.support.WebClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory


@SpringBootApplication(proxyBeanMethods = false)
class WebfluxApplication {

    val logger: Logger = LoggerFactory.getLogger(WebfluxApplication::class.java)

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<WebfluxApplication>(*args)
        }
    }

    @Bean
    fun readyListener(registry: ObservationRegistry, client: HelloClient) = ApplicationListener<ApplicationReadyEvent> { event ->

        Observation
                .createNotStarted("hello.client", registry)
                .lowCardinalityKeyValue("GreetingType", "Salutation")
                .contextualName("ready-listener")
                .observe {
                    logger.info("sending a Salutation request")

                    client.hello("C3PO")
                            .map{it.greeting}
                            .doOnNext(logger::info)
                            .block()
                }
    }


    @Bean
    fun helloClient(builder: WebClient.Builder) =
            HttpServiceProxyFactory
                    .builder(WebClientAdapter.forClient(builder.baseUrl("http://localhost:8787").build()))
                    .build()
                    .createClient(HelloClient::class.java)

    @Bean
    fun registry(): ObservationRegistry = ObservationRegistry.create()

    @Bean
    fun observedAspect(observationRegistry: ObservationRegistry): ObservedAspect =
            ObservedAspect(observationRegistry)
}