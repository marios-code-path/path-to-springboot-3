package com.example.rsocket.clients

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler
import java.util.*

@SpringBootApplication
class RsocketApplication {

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			runApplication<RsocketApplication>(*args)
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
}