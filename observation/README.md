# Observation and ProblemDetails

This section detials routines for establishing a traced and metered web application using SpringMVC

# Observation

With Spring Boot 3.0, we have new support for [Micrometer Tracing](https://micrometer.io/docs/tracing) which replaces 
 [Spring Cloud Sleuth](https://spring.io/projects/spring-cloud-sleuth) support. There is a great [writeup](https://spring.io/blog/2022/10/12/observability-with-spring-boot-3) on this
already, which takes care of explaining most of the details mentioned here.

## Why replace Sleuth?

Quotes Starbuxman: "So spring framework couldnt support sleuth since it was built on top of spring cloud, and spring cloud built on spring boot, and spring boot built on spring framework. There would be a circular dependency " 
# Problems