package com.example.webflux.clients

import io.micrometer.core.annotation.Timed
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry
import io.micrometer.observation.aop.ObservedAspect
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.aop.SpringProxy
import org.springframework.aop.framework.Advised
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ImportRuntimeHints
import org.springframework.core.DecoratingProxy
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.support.WebClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory


@EnableScheduling
@SpringBootApplication(proxyBeanMethods = false)
class WebfluxApplication {

    val log: Logger = LoggerFactory.getLogger(WebfluxApplication::class.java)

    @Bean
    @ImportRuntimeHints(ClientRuntimeHintsRegistrar::class)
    @RegisterReflectionForBinding(Salutation::class)
    fun helloClient(builder: WebClient.Builder) =
            HttpServiceProxyFactory
                    .builder(WebClientAdapter.forClient(builder.baseUrl("http://localhost:8787").build()))
                    .build()
                    .createClient(SalutationClient::class.java)

    @Bean
    fun registry(): ObservationRegistry = ObservationRegistry.create()

    @Bean
    fun observedAspect(observationRegistry: ObservationRegistry): ObservedAspect =
            ObservedAspect(observationRegistry)
}

fun main(args: Array<String>) {
    runApplication<WebfluxApplication>(*args)
}

@ImportRuntimeHints(ClientRuntimeHintsRegistrar::class)
@Configuration
class HostCaller(val registry: ObservationRegistry, val client: SalutationClient) {
    val log: Logger = LoggerFactory.getLogger(HostCaller::class.java)

    @Timed()
    @Scheduled(initialDelay = 2000, fixedRate = 2000)
    fun callHost() {
//        val obs = Observation.createNotStarted("reactive-hello", registry)
//                .lowCardinalityKeyValue("GreetingType", "Salutation")
//                .contextualName("reactive-call-host")
//                .start()
//
//        client.hello("C3PO")
//                .doOnNext { log.info(it.greeting) }
//                .doOnTerminate{
//                    obs.stop()
//                }
//                .subscribe()
//
        Observation
                .createNotStarted("hello.client", registry)
                .lowCardinalityKeyValue("GreetingType", "Salutation")
                .contextualName("call-host")
                .observe {
                    log.info("sending a Salutation request")

                    client.entityHello("C3PO")
                            .doOnNext {res ->
                                val sal = res.body!!
                                log.info("${res.statusCode}: ${sal.greeting}")

                            }
                            .block()
                }
    }
}

object ClientRuntimeHintsRegistrar : RuntimeHintsRegistrar {
    override fun registerHints(hints: RuntimeHints, classLoader: ClassLoader?) {
        hints.proxies().registerJdkProxy(
                SalutationClient::class.java,
                SpringProxy::class.java,
                Advised::class.java,
                DecoratingProxy::class.java
        )
    }
}