package com.example.rsocket.clients

import org.springframework.web.service.annotation.GetExchange
import reactor.core.publisher.Mono
import java.util.*


interface HelloClient {
	@GetExchange("/hello/{name}")
	fun hello(lang: Optional<String>,
              name: String)
			: Mono<String>

	@GetExchange("/names")
	fun names(): Mono<String>
}