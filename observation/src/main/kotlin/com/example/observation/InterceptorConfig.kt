package com.example.observation

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.ui.ModelMap
import org.springframework.web.context.request.WebRequest
import org.springframework.web.context.request.WebRequestInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.handler.WebRequestHandlerInterceptorAdapter


//@Configuration
class InterceptorConfig : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(WebRequestHandlerInterceptorAdapter(HelloInterceptor()))
    }
}

class HelloInterceptor : WebRequestInterceptor {
    val log: Logger = LoggerFactory.getLogger(HelloInterceptor::class.java)

    override fun preHandle(request: WebRequest) {
        log.info("Pre-Handle")
    }

    override fun postHandle(request: WebRequest, model: ModelMap?) {
        log.info("Post-Handle")
    }

    override fun afterCompletion(request: WebRequest, ex: Exception?) {
        log.info("After-complete")
    }

}