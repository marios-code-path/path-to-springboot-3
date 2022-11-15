package com.example.webflux.clients

import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor
import io.micrometer.tracing.handler.TracingObservationHandler.TracingContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.support.WebClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import reactor.core.observability.micrometer.Micrometer
import reactor.core.publisher.Signal
import java.util.function.Consumer


@EnableScheduling
@SpringBootApplication(proxyBeanMethods = false)
class WebfluxApplication {

    @Bean
    //  @ImportRuntimeHints(ClientRuntimeHintsRegistrar::class)
    // @RegisterReflectionForBinding(Greeting::class)
    fun greetingClient(builder: WebClient.Builder,
                       registry: ObservationRegistry): GreetingClient {
        return HttpServiceProxyFactory
                .builder(WebClientAdapter.forClient(
                        builder.baseUrl("http://localhost:8787")
                                .build()))
                .build()
                .createClient(GreetingClient::class.java)
    }
}

fun main(args: Array<String>) {
    runApplication<WebfluxApplication>(*args)
}

//@ImportRuntimeHints(ClientRuntimeHintsRegistrar::class)
@Configuration
class HostCaller(val registry: ObservationRegistry, val client: GreetingClient) {
    val log: Logger = LoggerFactory.getLogger(HostCaller::class.java)


    fun <T> logOnNext(logStmt: Consumer<T?>): Consumer<Signal<T>> =
            Consumer { signal: Signal<T> ->
                if (signal.isOnNext) {
                    val maybeObservation = signal.contextView.getOrEmpty<Observation>(ObservationThreadLocalAccessor.KEY)
                    maybeObservation.ifPresentOrElse({ observation: Observation ->
                        MDC.putCloseable("traceId",
                                (observation.context.get<Any>(TracingContext::class.java) as TracingContext?)!!.span.context().traceId()
                        ).use { cmdc -> logStmt.accept(signal.get()) }
                    }) { logStmt.accept(signal.get()) }
                }
            }


    @Scheduled(initialDelay = 2000, fixedRate = 2000)
    fun callHost() = client.hello("c3po")
            .tap(Micrometer.observation(registry))
            .name("hello.client.reactive")
            .tag("GreetingType", "ReactiveSalutation")
            .doOnEach(logOnNext { c -> log.info("message is: $c") })
            .subscribe()

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