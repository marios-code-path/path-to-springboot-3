# Reactive Tracing with Spring Boot 6

With Spring Boot 3.0, we reap the benefits of cutting edge changes with Spring Framework 6. This gives us a plethora of new and enhanced observability functionality at our disposal.  You might wander if this feature has already existed, and why this is considered new. It is not new, but rather re-tooling the existing Sleuth logic into micrometer. Furthermore, there are architectural framework decisions that impinge upon this change. As such, a quote from a Spring luminary is necessary: 

> **_QUOTE:_** Starbuxman: "So spring framework couldn't support Sleuth since it was built on top of spring cloud, and spring cloud built on spring boot, and spring boot built on spring framework. There would be a circular dependency "

In this guide, we will take a look at the updated support for [Micrometer Tracing](https://micrometer.io/docs/tracing), which replaces [Spring Cloud Sleuth](https://spring.io/projects/spring-cloud-sleuth) support. There is a great [writeup](https://spring.io/blog/2022/10/12/observability-with-spring-boot-3) on this already, which takes care of explaining a good chunk of details.

Additionally, if you are looking to migrate, please see [this WIKI](https://github.com/micrometer-metrics/micrometer/wiki/Migrating-to-new-1.10.0-Observation-API)
as it describes and gives samples related to the scenarios you will encounter when deciding/making the change from Sleuth to the new Micrometer API.

# Quick Setup

We will begin by starting a small REST controller application. It can be started by either starting at our favorite website - [start dot spring dot io](https://start.spring.io) - ther by selecting a few options:

Dependencies: 

  * Web
  * Actuator
  * Prometheus

Platform Version:

  * 3.0.0

Packaging: 

  * Jar

JvmVersion:

  * 17

Type:

  * Maven

Here is a screenshot (for reference) of what the configuration on `start.spring.io` looks like:

![start dot spring io](images/start-spring-io.png)

Alternatively, if you have the `curl`  client installed then copy and paste the following script as it will execute a similar function from the commandline.


```shell
curl -G https://start.spring.io/starter.zip -o observable.zip -d dependencies=web,actuator,prometheus -d packageName=com.example.observation \
-d description=REST%20Observation%20Demo -d type=maven-project -d language=java -d platformVersion=3.0.0-SNAPSHOT \
-d packaging=jar -d jvmVersion=17 -d groupId=com.example -d artifactId=observation -d name=observation  
```


Open this project in your favorite IDE and follow along, or simply [browse]() the source for reference.

To make the next part more obvious, we will describe the basic configuration of our server; app name, server port and logging format.

application.properties:
```properties
spring.application.name=test-service
server.port=8787
logging.pattern.level=%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]
```

## A Greeting Service

A service will let us create a small amount of indirection for the evenentual REST endpoint, and allow us to establish service hand-offs in tracing logs. In this example, we only need
to return a specific payload: `Greeting`.

The payload is a simple record:
```java
record Greeting(String name) {}
```

The following service code represents the service we'll use in a REST Controller later:
```java
@Service
@Slf4j
class GreetingService {
    private final LatencySupplier latency = new LatencySupplier();
    private final ObservationRegistry registry;

    GreetingService(ObservationRegistry registry) {
        this.registry = registry;
    }

    public Mono<Greeting> greeting(String name) {

        Long lat = latency.get();
        return Mono
                .just(new Greeting(name))
                .delayElement(Duration.ofMillis(lat))
                .doOnNext(g -> log.info(String.format("Latency: %d", lat)))
                ;
    }
}
```

Note the `LatencySupplier` class declared;  we will use this in order to demonstrate micrometer latency timers in our monitoring tool front-end [Grafana](https://grafana.com). 

LatencySupplier.java
```java
class LatencySupplier implements Supplier<Long> {
 
    @Override public Long get() {
        return new Random(System.currentTimeMillis()).nextLong(250);
    }
}
```
## Create a REST endpoint

Next, we will need to add a REST endpoint that simply vends salutations/greetings to a name derived from the path parameter {name}. It looks like this:

```kotlin
@RestController
class GreetingController {
    private final GreetingService service;
 
    GreetingController(GreetingService service) { this.service = service; }

    @GetMapping("/hello/{name}")
    public Mono<Greeting> greeting(@PathVariable("name") String name) {
        return service
                .greeting(name);
    }
}```
# Application Monitoring

For the application to be considered 'production ready', we must include some amount of monitoring features that indicate how the app is doing at runtime. Luckily, Spring Boot Actuator provides all of Spring Boot's monitoring features.

> **_TIP:_** Because so much of the monitoring information can also be used against your app, as well as add unnecessary data density, it is a good idea 
to review [the Actuator docs](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html).
This will be a great starting place if you are already new to Actuator, or just want to know how things
can be turned on or off, apply security, etc...

Since Spring Framework 6, metrics and tracing get handled by [Micrometer](https://micrometer.io).  This framework is a vendor-neutral 
API for instrumenting code and sending measurements to aggregators such as Prometheus, InfluxDB, Netflix Atlas 
and more. Furthermore, Spring Actuator and Micrometer work together - Micrometer gathers metrics, and Actuator will release this information through endpoints.

## Enable Prometheus support

We will need to tell Actuator we want exposure of Prometheus bound metric data. 
Let's configure our app to expose the specific `/actuator/prometheus` endpoint:

```properties
management.endpoints.web.exposure.include=health,prometheus
```

(If you dont already have prometheus installed, see [this]() infrastructure configuration for hints)
On the Prometheus side, we will need to add a [scrape config](https://prometheus.io/docs/prometheus/latest/configuration/configuration/)
to let it know where to find our actuator endpoint. 

> **_NOTE:_** In this configuration, we are running a docker hosted instance of Prometheus; You may need to change the host names as needed for your specific setup.

The `scrape config` block goes into `prometheus.yml`:
```yaml
scrape_configs:
    - job_name: 'spring-apps'
      metrics_path: '/actuator/prometheus'
      static_configs:
        - targets: ['host.docker.internal:8787']
```


## A bit about Micrometer Observation

The biggest change for micrometer in Spring Framework 6 is the addition of version 1.10's [Observation](https://micrometer.io/docs/observation) API.
This will enable a broader range of metrics/tracings instrumentation in your code. As we are starting fresh, we can make use of this low-friction API. 

To understand how observation gets 'handled', Micrometer employs "handlers" that are notified about the lifecycle event of an
observation. The default autoconfiguration will include several [ObservationHandlers]() that are registered to an [ObservationRegistry]().
These handlers wrap our observed code with the instrumentation needed to fulfill metrics and tracing objectives.  

An `ObservationHandler` reacts only to supported implementations of an `Observation.Context` and can create,
for example, timers, spans, and logs by reacting to the lifecycle events of an observation, such as:

* `start` - Observation has been started. Happens when the `Observation#start()` method gets called.
* `stop` - Observation has been stopped. Happens when the `Observation#stop()` method gets called.
* `error` - An error occurred while observing. Happens when the `Observation#error(exception)` method gets called.
* `event` - An event happened when observing. Happens when the `Observation#event(event)` method gets called.
* `scope started` - Observation opens a scope. The scope must be closed when no longer used. Handlers can create thread local variables on start that are cleared upon closing of the scope. Happens when the `Observation#openScope()` method gets called.
* `scope stopped` - Observation stops a scope. Happens when the `Observation.Scope#close()` method gets called.

## Observing our app

We want to instrument any request to `/hello/{name}` with traces/spans, and timers for latency. One way to do this
is by annotating a method with [@Observed](). Per the doc;  You can put that annotation either on a method to observe
it or a class to observe all methods in it.

First, we will enable the AspectJ advice for intercepting types or methods annotated with @Observed.

We can declare bean [ObservedAspect]() for this:

```kotlin
    @Bean
    fun observedAspect(observationRegistry: ObservationRegistry): ObservedAspect =
            ObservedAspect(observationRegistry)
```

We will then decorate our service method with `@Observed` and explain the parameters below:

```kotlin
@Service
class HelloService {
    val log: Logger = LoggerFactory.getLogger(HelloService::class.java)

    @Observed(name = "greeting", contextualName = "get-greeting", 
            lowCardinalityKeyValues = ["GreetingType", "Salutation"])
    fun getHello(name: String): Greeting {

        if (StringUtils.hasText(name) &&
                name[0].isDigit()) {
            throw IllegalArgumentException("Invalid name format.")
        }

        Thread.sleep(200)

        return Greeting("HELLO THERE, $name at ${System.currentTimeMillis()}")
    }
}
```

With metrics and tracing on the classpath, having this annotation leads to the creation of a `timer`, 
a `long task timer`, and a `span`. The timer would be named `greeting`, 
the long task timer would be named `greeting.active`, and the span would be named `get-greeting`.

### A Custom Handler

We can output some logs when a request is received and completed. As mentioned above, this is possible by
implementing a custom `ObservationHandler`. 

The following `ObservationHandler` will be registered and active during the server lifecycle:
```kotlin
@Component
class HelloObservationHandler : ObservationHandler<Observation.Context> {

    val logger: Logger = LoggerFactory.getLogger(HelloObservationHandler::class.java)

    override fun onStart(context: Observation.Context) {
        logger.info("Started observation of ${context.name}")
    }

    override fun onStop(context: Observation.Context) {
        logger.info("Stopped observation for ${context.name}")
    }

    override fun supportsContext(context: Observation.Context): Boolean = true
}
```


Execute this application, then call the exposed endpoint endpoint using our favorite [HTTPIE]() command:

```shell
http :8787/hello/Mr\ \.Kenobi
```

This verifies we created an app that is MOSTLY instrumented...
# Exemplars



## Links and Readings

[Spring Metrics Docs](https://docs.spring.io/spring-metrics/docs/current/public/prometheus)

[Issue detailing support for ProblemDetails](https://github.com/spring-projects/spring-framework/issues/27052)

[RFC 7807](https://www.rfc-editor.org/rfc/rfc7807)

[Observability Writeup](https://spring.io/blog/2022/10/12/observability-with-spring-boot-3)

[Observability Migration from Sleuth](https://github.com/micrometer-metrics/micrometer/wiki/Migrating-to-new-1.10.0-Observation-API)

[Should I use the Pushgateway?](https://prometheus.io/docs/practices/pushing/)