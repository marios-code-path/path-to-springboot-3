package com.example.rsocket.clients

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class RsocketApplication

fun main(args: Array<String>) {
	runApplication<RsocketApplication>(*args)
}
