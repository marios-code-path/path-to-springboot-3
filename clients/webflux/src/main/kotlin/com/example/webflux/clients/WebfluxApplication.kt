package com.example.webflux.clients

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.support.WebClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import java.util.*

@SpringBootApplication
class WebfluxApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<WebfluxApplication>(*args)
        }
    }

    @Bean
    fun readyListener(client: HelloClient) = ApplicationListener<ApplicationReadyEvent> {
        event ->
                client.hello(Optional.empty(), "User")
                        .doOnNext { hello ->
                            println("Message: $hello")
                        }
                        .block()

                client.names()
                        .doOnNext(::println)
                        .block()
    }

    @Bean
    fun helloClient(builder: WebClient.Builder) =
        HttpServiceProxyFactory
                .builder(WebClientAdapter.forClient(builder.baseUrl("http://localhost:8080").build()))
                .build()
                .createClient(HelloClient::class.java)
}