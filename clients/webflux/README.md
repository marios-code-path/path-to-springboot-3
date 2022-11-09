# Declarative Clients with WebFlux

When it comes to configuring a client to talk to our HTTP services, a challenge may
become apparent when detailing many calls. You might have the idea of creating re-usable
blocks only for one of those blocks to break due to specific calling options. While
using [WebClient]() is not entirely the worst situation, it can be better. 

With [Spring Framework 6](), we get the introduction of a Proxied service client 
that enables declarative configuration for client HTTP calls. This removes the aforementioned
boilerplate we so enjoy debugging and refactoring.

## Define a service

## Define a classic client

## Define a HttpProxy client

### Different Verbs


## Observing the Client calls

### Actuator with Prometheus

### 

## Links

[Spring 6 Deprecation List](https://docs.spring.io/spring-framework/docs/current-SNAPSHOT/javadoc-api/deprecated-list.html)

[Spring Metrics Docs](https://docs.spring.io/spring-metrics/docs/current/public/prometheus)
