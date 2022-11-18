# Reactive Tracing with Spring Boot 6

With Spring Boot 3.0, we reap the benefits of cutting edge changes with Spring Framework 6. This gives us a plethora of new and enhanced observability functionality at our disposal.  You might wander if this feature has already existed, and why this is considered new. It is not new, but rather re-tooling the existing Sleuth logic into micrometer. Furthermore, there are architectural framework decisions that impinge upon this change. As such, a quote from a Spring luminary is necessary: 

> **_QUOTE:_** Starbuxman: "So spring framework couldn't support Sleuth since it was built on top of spring cloud, and spring cloud built on spring boot, and spring boot built on spring framework. There would be a circular dependency "

In this guide, we will take a look at the updated support for [Micrometer Tracing](https://micrometer.io/docs/tracing), which replaces [Spring Cloud Sleuth](https://spring.io/projects/spring-cloud-sleuth) support. There is a great [writeup](https://spring.io/blog/2022/10/12/observability-with-spring-boot-3) on this already, which takes care of explaining a good chunk of details.

Additionally, if you are looking to migrate, please see [this WIKI](https://github.com/micrometer-metrics/micrometer/wiki/Migrating-to-new-1.10.0-Observation-API)
as it describes and gives samples related to the scenarios you will encounter when deciding/making the change from Sleuth to the new Micrometer API.

## Application Monitoring

For an application to be considered 'production ready', we must include some amount of monitoring features that indicate how the app is doing at runtime. Luckily, Spring Boot Actuator provides most of Spring Boot monitoring features.

> **_TIP:_** Because so much of the monitoring information can also be used against your app, as well as add unnecessary data density, it is a good idea to review [the Actuator docs](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html). This will be a great starting place if you are already new to Actuator, or just want to know how to control endpoint exposure or apply security among other things.

## More about Micrometer Observation

Since Spring Framework 6, metrics and tracing get handled by [Micrometer](https://micrometer.io) - a vendor-neutral API for instrumenting code. Micrometer also makes available and sends measurements to aggregators such as [Prometheus](https://prometheus.io), [InfluxDB](https://influxdata.com), [Netflix Atlas](https://netflix.github.io/atlas-docs/overview/) and more. Furthermore, Spring Actuator and Micrometer work together - Micrometer gathers metrics and makes them available on `management` endpoints via Actuator.

** WHAT AM I TALKING ABOUT ??? **
Micrometer makes use of a number of observation API's to instrument our code. For example, You may employ a [Meter]() to count the number of requests per second in your web app, or a [histogram] to determine `n`-percentile for latency. One way to enable multiple observation criteria is to use Micrometer's [Observation]() API.
** WHAT AM I TALKING ABOUT ??? **

### First, a Simple

There is a quick and simple way to add observation to your Spring-Boot 3 app. We simply need to wrap imperative code with one of the instrumentation APIS - in this case, the highly versatile [Observation](https://github.com/micrometer-metrics/micrometer/blob/main/micrometer-observation/src/main/java/io/micrometer/observation/Observation.java) API.

SimpleObservationApplication.java:

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
        String something = Observation
                .createNotStarted("server.job", registry) // 1
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
 1. Create an instance of `Observation` bound to an `ObservationRegistry` as stated in [The Doc](https://micrometer.io/docs/observation).
 2. To better track our invocation, set [Low Cardinality keys]() - that is keys which have little or no variations in value. For [High Cardinality]() - which is many value possibilities - use the `.highCardinalityKeyValue()` method.
 3. Rather than manually calling `.start()` and `.stop()`, use the [observe(Runnable)] to isolate any monitored code in it's own `Runnable` closure.
### How Observation works

Micrometer Observation employs "handlers" that are notified about the lifecycle event of an observation. An `ObservationHandler` handler wraps around the [Observation] lifecycle and execute it's methods on lifecycle events. An `ObservationHandler` reacts only to supported implementations of an `Observation.Context` and can create, for example, timers, spans, and logs by reacting to the lifecycle events of an observation, such as:

* `start` - Observation has been started. Happens when the `Observation#start()` method gets called.
* `stop` - Observation has been stopped. Happens when the `Observation#stop()` method gets called.
* `error` - An error occurred while observing. Happens when the `Observation#error(exception)` method gets called.
* `event` - An event happened when observing. Happens when the `Observation#event(event)` method gets called.
* `scope started` - Observation opens a scope. The scope must be closed when no longer used. Handlers can create thread local variables on start that are cleared upon closing of the scope. Happens when the `Observation#openScope()` method gets called.
* `scope stopped` - Observation stops a scope. Happens when the `Observation.Scope#close()` method gets called.

The default autoconfiguration will create at least an `ObservationRegistry` which is responsable for managing the state of Observations. Additionally, we get multiple `ObservationHandlers` that handle various instrumentation strategies (e.g. tracing, metrics, logging, etc..). For this example, lets declare one more handler that will log on each Observation lifecycle stage - [ObservationTextPublisher](https://github.com/micrometer-metrics/micrometer/blob/main/micrometer-observation/src/main/java/io/micrometer/observation/ObservationTextPublisher.java).

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

The example will use a REST endpoint which calls a service - a small amount of indirection, allowing us to establish service hand-offs in tracing logs. In this example, we only return a specific payload: `Greeting`.

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

Next, we will add a REST endpoint that returns a salutations/greetings to a name derived from the path parameter {name}. It looks like this:

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

## Reactive stream Observation and Metrics

We will make use of the reactive `tap` operators to instrument the streams in this sample. The `tap` operator makes use of a stateful per-subscription [SignalListener]() to manage the state of the observation in progress.

To get an micrometer signal listener, import the [reactor-core-micrometer](https://github.com/reactor/reactor-core/tree/main/reactor-core-micrometer) dependency. This API provides all of the necessary components that supply Observation and Meter metrics gathering in Reactive streams. Note, that this API also relies on [context-propagation](https://micrometer.io/docs/contextPropagation) to populate thread locals around the opening of Observations/Meters upon stream subscription.

Here are the additions we will add to pom.xml to enable reactive stream observation:

```xml
		<!-- Micrometer API -->
		<dependency>
			<groupId>io.projectreactor</groupId>
			<artifactId>reactor-core-micrometer</artifactId>
		</dependency>
		<dependency>
			<groupId>io.micrometer</groupId>
			<artifactId>context-propagation</artifactId>
			<version>1.0.0</version>
		</dependency>
```

In this example, we are interested in the `reactor.core.observability.micrometer.Micrometer` API that provides us the StreamListener needed to observe the stream. This API supports instrumentation of `reactor.core.scheduler.Scheudler`'s, as well as applying Meters and Observations on a per-subscription basis to the reactive stream. This example will observe our stream using the `Micrometer.observation` API.

GreetingService.java:
```java
        return Mono
                .just(new Greeting(name))
                .delayElement(Duration.ofMillis(lat))
                .name("greeting.call")                  // 1
                .tag("latency", lat.toString())         // 2
                .tap(Micrometer.observation(registry))  // 3
```

Given the above, we will have a child span for the parent HTTP controller one. The main additions are as follows:

1. Using `.name` to specify the `Observation` name.
2. Low cardinality tags add attributes to our measurements.
3. Produce the `Observation` specific signal listener. This covers the entire length of the sequence.

The core details of how you can further work in micrometer metrics into your streams can be read at the [Micrometer Observation Docs](https://micrometer.io/docs/observation).

## Execute the app

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

This might take a minute or two since containers need to be transferred over the network. Next, we can move on and examine this infrastructure.

## Prometheus Setup

One of the Prometheus side, we have [scrape config](https://prometheus.io/docs/prometheus/latest/configuration/configuration/) that configures ingest of the actuator endpoint. 

> **_NOTE:_** In this configuration, we are running a docker hosted instance of Prometheus; You may need to change the host names as needed for your specific setup.

The specific `scrape config` block goes into `infra/docker/prometheus/prometheus.yml`:
```yaml
scrape_configs:
    - job_name: 'spring-apps'
      metrics_path: '/actuator/prometheus'
      static_configs:
        - targets: ['host.docker.internal:8787']
```

To get data into the format that Prometheus can perceive, we already added the necessary dependency when we were in `start.
spring.io`:
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

The following configuration adds percentile histogram buckets for `http.server.requests` which is the prefix to the stats names that represents `WebMVC`/`WebFlux` requests. Additionally we want to create another for our own `server.job` statistics.

In `application.properties`, add:
```properties
management.metrics.distribution.percentiles-histogram.http.server.requests=true
management.metrics.distribution.percentiles-histogram.server.job=true
```
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

And the source to our `logback-spring.xml` should be found under `resources`:

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

This example will use [Micrometer tracing](https://micrometer.io/docs/tracing) that ships traces over to Tempo. With help of [Openzipkin Brave](https://github.com/openzipkin/zipkin-reporter-java/tree/master/brave) and the [micrometer bridge](https://github.com/micrometer-metrics/tracing/tree/main/micrometer-tracing-bridges) for Brave, we can ship traces to Tempo directly from Micrometer. In order to use this, however we must include both the bridge and the underlaying API that bridge will use.

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

Grafana will be provisioned with external data given from configuration within `infra/docker/grafana/provisioning/datasources/datasource.yml`. This tells grafana where to find each external source of data we will be querying. We will be tracking spans from Tempo, logs from Loki and Metrics from Prometheus.

To connect visualization with Prometheus, we will configure our dashboards with Prometheus [queries](https://prometheus.io/docs/prometheus/latest/querying/basics/). This dashboard will visualize our metric/span data and enable us to select a specific sample - an [exemplar](https://grafana.com/docs/grafana/v9.0/basics/exemplars/) - to gather a in-depth view into the process that occured at that sample point.

This dashboard configuration is provided in `infra/docker/grafana/provisioning/dashboards/logs_traces_metrics.json` and acts as our standard example dashboard called `logs_traces_metrics`.

# Observing the Reactive WebFlux app

[WebFluxObservationAutoConfiguration]() is the autoconfiguration class for observation in WebFlux. It includes everything 
we need to observe (draw traces and meters from) HTTP requests and responses, thus no additional configuration is necessary. 

We now can execute the application as-is:

```shell
mvn spring-boot:run
```

Once the application is started, a number of metrics will be populated because of scraping activity from Prometheus to the `/actuator/prometheus` endpoint. To add our own service activity, lets create some traffic.

```bash
while true; do http :8787/hello/spring-boot-3 ; sleep 1; done
```

This will call our endpoint every second. Allow it run for a minute or two so Prometheus can collect metrics. Then browse over to `http://localhost:3000/dashboards` and select 'General', then 'Logs, Traces, Metrics' dashboard. You would be facing a screen similar to the following screenshot:

![dashboard 1st](images/first-dashboard-screen.png)

Notice, we have a several gray squares - these are the `exemplars` we described earlier. The exemplar data is located in a hovering DIV and cooresponds to the details matched between that particular metric and a trace.

![exemplar](images/exemplar-data.png)

 The trace can be searched for in Loki at the top of this dashboard, for which Tempo data will be displayed. Clicking on 'Query with Tempo' will produce similar information.

![dashboard 2nd](images/second-dashboard-screen.png)

A full trace view can be expanded. Note we also see the trace generated through our service
call - the reactive stream observation made as a child trace to the main HTTP request.

![dashboard full](images/full-trace-view.png)

## Links and Readings

[Spring Metrics Docs](https://docs.spring.io/spring-metrics/docs/current/public/prometheus)

[Issue detailing support for ProblemDetails](https://github.com/spring-projects/spring-framework/issues/27052)

[RFC 7807](https://www.rfc-editor.org/rfc/rfc7807)

[Observability Writeup](https://spring.io/blog/2022/10/12/observability-with-spring-boot-3)

[Observability Migration from Sleuth](https://github.com/micrometer-metrics/micrometer/wiki/Migrating-to-new-1.10.0-Observation-API)

[Should I use the Pushgateway?](https://prometheus.io/docs/practices/pushing/)

[Jack Neeley's Blog on Exemplar :)](https://linuxczar.net/blog/2022/01/17/java-spring-boot-prometheus-exemplars/)

[Prometheus Documentation](https://prometheus.io/docs/introduction/overview/)