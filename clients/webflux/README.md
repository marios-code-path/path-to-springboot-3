# Declarative Clients with WebFlux

When it comes to configuring a client to talk to our HTTP services, a challenge may
become apparent when detailing many calls. You might have the idea of creating re-usable
blocks only for one of those blocks to break due to specific calling options. While
using [WebClient]() is not entirely the worst situation, it can be better. 

With [Spring Framework 6](), we get the introduction of a Proxied service client 
that enables declarative configuration for client HTTP calls. This removes the aforementioned
boilerplate we so enjoy debugging and refactoring.

## Define a service

The service can be any interface with the signature needed to provide arugments into
HTTP requests. We can illustrate this in the following example:

```kotlin
interface SalutationClient {
    @GetExchange("/hello/{name}", accept = [MediaType.APPLICATION_JSON_VALUE])
    fun hello(@PathVariable name: String): Mono<Salutation>

    @GetExchange("/hello/{name}", accept = [MediaType.APPLICATION_JSON_VALUE])
    fun entityHello(@PathVariable name: String): Mono<ResponseEntity<Salutation>>
}
```

In this example it is obvious about what kind of request will be made. In addition to 
verbs, we will also send the media types accepted. The `ResponseEntity` return type
is useful when you want your client to have access to the request result status metadata.


## Define a classic client

 TBD maybe not. We already know what a classic restTemplate/ webClient client looks like!

## Define a HttpProxy client

Now, with just a little configuration, we can setup the client to read `org.springframework.web.service.annotation`
annotated methods. This introduces some introspection at runtime, but does all the work for transforming our interface
signature into real HTTP calls.

```kotlin
    @Bean
    fun helloClient(builder: WebClient.Builder) =
            HttpServiceProxyFactory
                    .builder(WebClientAdapter.forClient(
                            builder
                                .baseUrl("http://localhost:8787")
                                .build()))
                    .build()
                    .createClient(SalutationClient::class.java)
```

### Calling the service

The calling side now looks like a regular Service call!

```kotlin
                    client.entityHello("C3PO")
                            .doOnNext {res ->
                                val sal = res.body!!
                                log.info("${res.statusCode}: ${sal.greeting}")

                            }
                            .block()
```

### 

## Links

[Spring 6 Deprecation List](https://docs.spring.io/spring-framework/docs/current-SNAPSHOT/javadoc-api/deprecated-list.html)