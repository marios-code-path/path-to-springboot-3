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
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@SpringBootApplication(proxyBeanMethods = false)
class ObservationApplication {

    @Bean
    fun observedAspect(observationRegistry: ObservationRegistry): ObservedAspect =
            ObservedAspect(observationRegistry)
}

// TODO The traces are also not communicated with AOT
// AOT wont work without this.
// https://github.com/spring-projects/spring-boot/issues/32918
fun main(args: Array<String>) {
    runApplication<ObservationApplication>(*args)
}

@ControllerAdvice
class ProblemDetailHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(java.lang.IllegalArgumentException::class)
    fun handleException(req: WebRequest,
                        except: IllegalArgumentException): ResponseEntity<Any> {

        val problemDetail = createProblemDetail(except,
                HttpStatus.BAD_REQUEST,
                "Provide a Name!",
                null,
                null,
                req)
        return createResponseEntity(problemDetail, HttpHeaders.EMPTY, HttpStatus.BAD_REQUEST, req)
    }
}

@RestController
class MyRestController(val svc: HelloService) {
    val logger: Logger = LoggerFactory.getLogger(HelloHandler::class.java)

    @RequestMapping("/hello/{name}")
    fun hello(@PathVariable("name") name: String): String {

        logger.info("Request for salutation")
        return svc.getHello(name)
    }
}

@Service
class HelloService {

    @Observed(name = "greeting",
            contextualName = "say-hi",
            lowCardinalityKeyValues = ["GreetingType", "Salutation"])
    fun getHello(name: String): String {

        if (StringUtils.hasText(name) &&
                name[0].isDigit()) {
            throw IllegalArgumentException("Please provide a valid name.")
        }

        Thread.sleep(2000)
        return "HELLO THERE. $name at " + System.currentTimeMillis()
    }
}

@Component
class HelloHandler : ObservationHandler<Observation.Context> {

    val logger: Logger = LoggerFactory.getLogger(HelloHandler::class.java)

    override fun onStart(context: Observation.Context) {
        logger.info("Started observation of ${context.name} with GreetingType !!!!")
    }

    override fun onStop(context: Observation.Context) {
        logger.info("Stopped observation for ${context.name} with GreetingType !!!")
    }

    override fun supportsContext(context: Observation.Context): Boolean = true
}