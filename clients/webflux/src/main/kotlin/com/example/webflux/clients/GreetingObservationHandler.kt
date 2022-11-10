package com.example.webflux.clients

import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class GreetingObservationHandler : ObservationHandler<Observation.Context> {
    val logger: Logger = LoggerFactory.getLogger(GreetingObservationHandler::class.java)

    override fun onStart(context: Observation.Context) {
        logger.info("Started observation of ${context.name}")
    }

    override fun onStop(context: Observation.Context) {
        logger.info("Stopped observation for ${context.name}")
    }

    override fun supportsContext(context: Observation.Context): Boolean {
        return true;
    }
}