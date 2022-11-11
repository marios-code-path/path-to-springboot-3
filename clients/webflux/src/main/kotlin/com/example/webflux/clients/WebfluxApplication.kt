package com.example.webflux.clients

import io.micrometer.observation.ObservationRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.support.WebClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory


//@EnableScheduling
@SpringBootApplication(proxyBeanMethods = false)
class WebfluxApplication {

    @Bean
  //  @ImportRuntimeHints(ClientRuntimeHintsRegistrar::class)
   // @RegisterReflectionForBinding(Greeting::class)
    fun greetingClient(builder: WebClient.Builder): GreetingClient {
        return HttpServiceProxyFactory
                .builder(WebClientAdapter.forClient(builder.baseUrl("http://localhost:8787").build()))
                .build()
                .createClient(GreetingClient::class.java)
    }

    @Bean
    fun registry(): ObservationRegistry = ObservationRegistry.create()

}

fun main(args: Array<String>) {
    runApplication<WebfluxApplication>(*args)
}

//@ImportRuntimeHints(ClientRuntimeHintsRegistrar::class)
@Configuration
class HostCaller(val registry: ObservationRegistry, val client: GreetingClient) {
    val log: Logger = LoggerFactory.getLogger(HostCaller::class.java)

   // @Scheduled(initialDelay = 2000, fixedRate = 2000)
    fun callHost() {
        client
                .hello("c3po")
//                .tap(Micrometer.observation(registry))
//                .name("hello.client.reactive")
//                .tag("GreetingType", "ReactiveSalutation")
                .doOnNext { res ->
                    log.info("${res.greeting}")
                }
                .doOnError(IllegalArgumentException::class.java) { x ->
                    log.info(x.message)
                }
                .subscribe()
    }

}
//
//object ClientRuntimeHintsRegistrar : RuntimeHintsRegistrar {
//    override fun registerHints(hints: RuntimeHints, classLoader: ClassLoader?) {
//        hints.proxies().registerJdkProxy(
//                GreetingClient::class.java,
//                SpringProxy::class.java,
//                Advised::class.java,
//                DecoratingProxy::class.java
//        )
//    }
//}