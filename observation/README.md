# Reactive Tracing with Spring Boot 6

With Spring Boot 3.0, we reap the benefits of cutting edge changes with Spring Framework 6. This gives us a plethora of new and enhanced observability functionality at our disposal.  You might wander if this feature has already existed, and why this is considered new. It is not new, but rather re-tooling the existing Sleuth logic into micrometer. Furthermore, there are architectural framework decisions that impinge upon this change. As such, a quote from a Spring luminary is necessary: 

> **_QUOTE:_** Starbuxman: "So spring framework couldn't support Sleuth since it was built on top of spring cloud, and spring cloud built on spring boot, and spring boot built on spring framework. There would be a circular dependency "

In this guide, we will take a look at the updated support for [Micrometer Tracing](https://micrometer.io/docs/tracing), which replaces [Spring Cloud Sleuth](https://spring.io/projects/spring-cloud-sleuth) support. There is a great [writeup](https://spring.io/blog/2022/10/12/observability-with-spring-boot-3) on this already, which takes care of explaining a good chunk of details.

Additionally, if you are looking to migrate, please see [this WIKI](https://github.com/micrometer-metrics/micrometer/wiki/Migrating-to-new-1.10.0-Observation-API)
as it describes and gives samples related to the scenarios you will encounter when deciding/making the change from Sleuth to the new Micrometer API.

## Application Monitoring

For an application to be considered 'production ready', we must include some amount of monitoring features that indicate how the app is doing at runtime. Luckily, Spring Boot Actuator provides most of Spring Boot monitoring features.

> **_TIP:_** Because so much of the monitoring information can also be used against your app, as well as add unnecessary data density, it is a good idea to review [the Actuator docs](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html).
This will be a great starting place if you are already new to Actuator, or just want to know how to control endpoint exposure or apply security among other things.

## More about Micrometer Observation

Since Spring Framework 6, metrics and tracing get handled by [Micrometer](https://micrometer.io).  his framework is a vendor-neutral API for instrumenting code and sending measurements to aggregators such as [Prometheus](https://prometheus.io), [InfluxDB](https://influxdata.com), [Netflix Atlas](https://netflix.github.io/atlas-docs/overview/) and more. Furthermore, Spring Actuator and Micrometer work together - Micrometer gathers metrics and makes them available on `management` endpoints via Actuator.

Micrometer makes use of a number of observation API's to instrument our code. For example, You may emply a [Meter]() to count the number of requests per second in your web app, you may employ a [histogram] to determine `n`-percentile for latency. One way to enable multiple observation criteria is to use Micrometer's [Observation]() API.

There is a quick and simple way to add observation to you Spring-Boot 3 app.

Quickstart:

```java
@Slf4j
@SpringBootApplication
public class SimpleObservationApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(SimpleObservationApplication.class, args);
    }

    @Bean
    public ApplicationListener<ApplicationStartedEvent> doOnStart() {
        return event -> {
            generateString();
        };
    }

    public void generateString() {
        String something = Observation.createNotStarted("server.job", registry) // 1
                .lowCardinalityKeyValue("jobType", "string") // 2                
                .observe(() -> {    // 3
                    log.info("Generating a String...");
                    try {
                        // do something taking time on the thread
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return "NOTHING";
                    }
                    return "SOMETHING";
                });

        log.info("Result was: " + something);
    }
}
```

A few things are happening in this code: 
 1. Create an instance of [Observation]() which aggregates trace, metrics instrumentation.
 2. To better track our invocation, set [Low Cardinality keys]() - that is keys which have little or no variations in value. For [High Cardinality]() - which is many value possibilities - use the `.highCardinalityKeyValue()` method.
 3. Rather than manually calling `.start()` and `.stop()`, use the [observe(Runnable)] to isolate any monitored code in it's own closure.

Micrometer employs "handlers" that are notified about the lifecycle event of an observation. An `ObservationHandler` handler wraps around the [Observation] lifecycle and execute it's methods on lifecycle events. An `ObservationHandler` reacts only to supported implementations of an `Observation.Context` and can create, for example, timers, spans, and logs by reacting to the lifecycle events of an observation, such as:

* `start` - Observation has been started. Happens when the `Observation#start()` method gets called.
* `stop` - Observation has been stopped. Happens when the `Observation#stop()` method gets called.
* `error` - An error occurred while observing. Happens when the `Observation#error(exception)` method gets called.
* `event` - An event happened when observing. Happens when the `Observation#event(event)` method gets called.
* `scope started` - Observation opens a scope. The scope must be closed when no longer used. Handlers can create thread local variables on start that are cleared upon closing of the scope. Happens when the `Observation#openScope()` method gets called.
* `scope stopped` - Observation stops a scope. Happens when the `Observation.Scope#close()` method gets called.

The default autoconfiguration will create at least an [ObservationRegistry]() which is responsable for managing the state of Observations. Additionally, we get multiple [ObservationHandlers]() that handle various instrumentation strategies (e.g. tracing, metrics, logging, etc..). We can declare one more handler, that will log on each Observation lifecycle stage - [ObservationTextPublisher]().

```java
@Component
public class TextPublishingObservationHandler extends ObservationTextPublisher { }
```

When we execute our `SimpleObservationApplication`, we will see logs that `ObservationTextPublisher` emitted. Timestamps are removed, while just 1 descriptive log line appear to keep things brief in this sample.

```log
INFO 90538 --- [           main] i.m.o.ObservationTextPublisher           : START - name='server.job', contextualName='null', error='null', lowCardinalityKeyValues=[jobType='string'], highCardinalityKeyValues=[], map=[]
INFO 90538 --- [           main] i.m.o.ObservationTextPublisher           :  OPEN - name='server.job', contextualName='null', error='null', lowCardinalityKeyValues=[jobType='string'], highCardinalityKeyValues=[], map=[class io.micrometer.core.instrument.Timer$Sample='io.micrometer.core.instrument.Timer$Sample@205bed61', class io.micrometer.core.instrument.LongTaskTimer$Sample='SampleImpl{duration(seconds)=5.60409E-4, duration(nanos)=560409.0, startTimeNanos=}']
INFO 90538 --- [           main] c.e.o.ManualObservationApplication       : Generating a String...
INFO 90538 --- [           main] i.m.o.ObservationTextPublisher           : CLOSE - name='server.job',...]
INFO 90538 --- [           main] i.m.o.ObservationTextPublisher           :  STOP - name='server.job',...]
INFO 90538 --- [           main] c.e.o.ManualObservationApplication       : Result was: SOMETHING
```

Our `ObservationTextPublisher` shows the various stages this Observation went through, along with it's metadata.  Notice that once a Observation scope gets started, that our logging also shows a `traceId` and `spanId`. The autoconfigured handlers such as a [TracingAwareMeteredObservationHandler](https://github.com/micrometer-metrics/tracing/tree/main/micrometer-tracing/src/main/java/io/micrometer/tracing/handler) - have already taken care of those objectives.

### Annotation support

Another approach to instrumenting applications works with annotations via [@Observed](https://github.com/micrometer-metrics/micrometer/tree/main/micrometer-observation/src/main/java/io/micrometer/observation/annotation). We can eschew the manual creation of the `Observation` object, by decorating the method declaration itself. 

For project setup, we need to include the `spring-boot-starter-aop` dependency. We can then create a support object called the [ObservedAspect](https://github.com/micrometer-metrics/micrometer/tree/main/micrometer-observation/src/main/java/io/micrometer/observation/aop). Aspects decorate code blocks with [Advice]() executing before, and or after the method which is ideal for cross-cutting concerns like metrics and tracing.

# The Reactive App

The main goal of this guide is to describe Observation in Reactive apps, thus we will take the liberty to describe the sample app and outline it's objectives as well as discuss Reactive components that are related to micrometer observation.

We will begin by starting a small REST controller application. It can be started by either starting at our favorite website - [start dot spring dot io](https://start.spring.io) - there by selecting a few options:

Dependencies: 

  * Webflux
  * Actuator
  * Prometheus
  * Lombok

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

Alternatively, if you have the `curl` client installed then copy and paste the following script as it will execute a similar function from the commandline.

```shell
curl -G https://start.spring.io/starter.zip -o observable.zip -d dependencies=web,actuator,prometheus -d packageName=com.example.observation \
-d description=REST%20Observation%20Demo -d type=maven-project -d language=java -d platformVersion=3.0.0-SNAPSHOT \
-d packaging=jar -d jvmVersion=17 -d groupId=com.example -d artifactId=observation -d name=observation  
```

Open this project in your favorite IDE and follow along, or simply [browse]() the source for reference.

Lets move on and establish some basic application properties; app name, server port and logging format.

application.properties:
```properties
spring.application.name=test-service
server.port=8787
logging.pattern.level=%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]
```

## A Greeting Service

A service will let us create a small amount of indirection for the REST endpoint, and allow us to establish service hand-offs in tracing logs. In this example, we only need to return a specific payload: `Greeting`.

The payload is a simple record:

Greeting.java:
```java
record Greeting(String name) {}
```

The following service code represents the service we'll use in a REST Controller later:

GreetingService.java:
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

Note the `LatencySupplier` class declared; we will use this in order to embelish service latency. 

LatencySupplier.java:
```java
class LatencySupplier implements Supplier<Long> {
 
    @Override public Long get() {
        return new Random(System.currentTimeMillis()).nextLong(250);
    }
}
```
## Create a REST endpoint

Next, we will need to add a REST endpoint that simply returns salutations/greetings to a name derived from the path parameter {name}. It looks like this:

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
}
```

Executing this application alone will not produce the observation output we need. So some more knowledge and program details are required before we should continue. The next section will describe the monitoring platform itself, and how it relates to the example program we just started.
# Introduction of the Monitoring Platform

In this guide, we will have the experience of a single page observation. With this idea going forward, we can move through logs, traces and metrics in one location. In order to achieve this kind of integration, we will make use of a specific `operations` service infrastructure:

Infra:

  * [Prometheus](https://prometheus.io) - Metrics

  * [Loki](https://grafana.com/go/webinar/getting-started-with-logging-and-grafana-loki/?pg=hp&plcmt=upcoming-events-3) - Log Aggregation

  * [Tempo](https://grafana.com/go/webinar/getting-started-with-tracing-and-grafana-tempo-emea/?pg=hp&plcmt=upcoming-events-2) - Trace Backend
  
  * [Grafana](https://grafana.com/grafana/) - Dashboard Visualization

For this example, there are pre-configured instances of Prometheus, Grafana, Tempo, and Loki located within the `infra` directory. Provided in this directory are the docker compose scripts, and server configuration files. You can bring the whole thing up with the following command:

```bash
cd infra/
docker compose up
```

This might take a minute or two since containers need to be transferred over the network. However, by the time it's done, you should see something like the following:

![aftermath of docker compose up](images/docker-compose-up.png)

At this point, we will spend the next few subsections examining this infrastructure.

## Prometheus Setup

One of the Prometheus side, we have [scrape config](https://prometheus.io/docs/prometheus/latest/configuration/configuration/) that lets it ingest our actuator endpoint. 

> **_NOTE:_** In this configuration, we are running a docker hosted instance of Prometheus; You may need to change the host names as needed for your specific setup.

The specific `scrape config` block goes into `infra/docker/prometheus/prometheus.yml`:
```yaml
scrape_configs:
    - job_name: 'spring-apps'
      metrics_path: '/actuator/prometheus'
      static_configs:
        - targets: ['host.docker.internal:8787']
```

We already added the necessary dependency when we were in `start.spring.io`:
```xml
		<dependency>
			<groupId>io.micrometer</groupId>
			<artifactId>micrometer-registry-prometheus</artifactId>
			<scope>runtime</scope>
		</dependency>
```
### Enable the Prometheus Actuator endpoint

We will need to tell Actuator to expose metrics for scraping by Prometheus. 
Let's configure our app to expose the specific `/actuator/prometheus` endpoint:

In `application.properties`, add:
```properties
management.endpoints.web.exposure.include=prometheus
# Alternately you can say
management.endpoint.prometheus.enabled=true
```

Also, because we are going to use support for `Exemplars` in Prometheus, we will need to enable histogram buckets for 
our named observations - that tie traceId to metric buckets. 

The following configuration adds percentile histogram buckets for `http.server.requests` which is the prefix to the stats names that represents `WebMVC`/`WebFlux`. Additionally we want to create another for our own `server.job` statistics.

In `application.properties`, add:
```properties
management.metrics.distribution.percentiles-histogram.http.server.requests=true
management.metrics.distribution.percentiles-histogram.server.job=true
```

There are more dependencies for each of our observation platform components and will review them in the next few subsections.
## Loki log aggregation config

We will configure a logback appender to emit our logs directly to Loki. Loki appenders are of the [loki4j](https://loki4j.github.io/loki-logback-appender/) variety and are implemented in `com.github.loki4j.logback.Loki4jAppender`.

Simply place `logback-spring.xml` into `src/main/resources` of the project and ensure the appender for loki has the right URL configured.

To make this work, we use the `loki-logback-appender` dependency as configured with maven:

```xml
		<dependency>
			<groupId>com.github.loki4j</groupId>
			<artifactId>loki-logback-appender</artifactId>
			<version>1.4.0-rc1</version>
		</dependency>
```

And the source ot our `logback-spring.xml` should be found under `resources`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/base.xml" />
    <springProperty scope="context" name="appName" source="spring.application.name"/>

    <appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
        <http>
            <url>http://localhost:3100/loki/api/v1/push</url>
        </http>
        <format>
            <label>
                <pattern>app=${appName},host=${HOSTNAME},traceID=%X{traceId:-NONE},level=%level</pattern>
            </label>
            <message>
                <pattern>${FILE_LOG_PATTERN}</pattern>
            </message>
            <sortByTime>true</sortByTime>
        </format>
    </appender>

    <root level="INFO">
        <appender-ref ref="LOKI"/>
    </root>
</configuration>
```

## Tempo - Micrometer Tracing

To get our traces over to tempo, this example will use [micrometer tracing](https://micrometer.io/docs/tracing) that ships traces over to Tempo.

With help of [Openzipkin Brave](https://github.com/openzipkin/zipkin-reporter-java/tree/master/brave) and the [micrometer bridge for Brave, we can ship traces to Tempo directly from micrometer. In order to use this, however we must include both the bridge and the underlaying API that bridge will use.

Dependencies to use Brave Tracing from micrometer:
```xml
		<dependency>
			<groupId>io.micrometer</groupId>
			<artifactId>micrometer-tracing-bridge-brave</artifactId>
		</dependency>
		<dependency>
			<groupId>io.zipkin.reporter2</groupId>
			<artifactId>zipkin-reporter-brave</artifactId>
		</dependency>
```
## Grafana dashboards

In this example, Grafana will be provisioned with external data given from configuration within `infra/docker/grafana/provisioning/datasources/datasource.yml`. This tells grafana where to find each external source of data we will be querying. We will be tracking spans from Tempo, logs from Loki and Metrics from Prometheus.

Probably most importantly are the dashboards we will use to visualize these queries. This configuration  - in addition to visual setup - describes the Prometheus [queries](https://prometheus.io/docs/prometheus/latest/querying/basics/) we will execute to visualize our metric data. This dashboard will visualize our metric data and enable us to select a specific sample - an [exemplar](https://grafana.com/docs/grafana/v9.0/basics/exemplars/) - to gather a in-depth view into the process that occured at that sample point.

Here is an example of the exemplar that connects a metric sample with trace_data, and therefore it's related logs:

![Exemplar](images/exemplar.png)

Therefore, the configuration for such are provided in `infra/docker/grafana/provisioning/dashboards/logs_traces_metrics.json` and acts as our standard example dashboard called `logs_traces_metrics`.

# Observing our app

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

[Jack Neeley's Blog on Exemplar :)](https://linuxczar.net/blog/2022/01/17/java-spring-boot-prometheus-exemplars/)

[Prometheus Documentation](https://prometheus.io/docs/introduction/overview/)