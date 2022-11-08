package com.example.observation

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.HandlerMapping
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler


@ControllerAdvice
class ProblemDetailHandler : ResponseEntityExceptionHandler() {

    /**
     * ErrorResponse is supported as a return value from @ExceptionHandler methods that render
     * directly to the response, e.g. by being marked @ResponseBody, or declared in an
     * @RestController or RestControllerAdvice class.
     */
    @ExceptionHandler(java.lang.IllegalArgumentException::class)
    fun handleException(req: WebRequest,
                        except: IllegalArgumentException): ResponseEntity<Any> {

        val attr = req.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST) as Map<String, String>
        val name = attr.getOrDefault("name", "-")

        val problemDetail = createProblemDetail(except,
                HttpStatus.BAD_REQUEST,
                "Exception: ${except.message}",
                null,   // e.g. problemDetail.custom
                arrayOf(name),
                req)
        return createResponseEntity(problemDetail, HttpHeaders.EMPTY, HttpStatus.BAD_REQUEST, req)
    }
}