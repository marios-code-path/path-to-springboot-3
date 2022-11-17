package com.example.observation.aop;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
import io.micrometer.observation.aop.ObservedAspect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;

@Slf4j
@SpringBootApplication(proxyBeanMethods = false)
public class AopObservationApplication {

    public static void main(String[] args) {
        SpringApplication.run(AopObservationApplication.class, args);
    }

    @Bean
    public ApplicationListener<ApplicationStartedEvent> doOnStart() {
        return event -> {
            try {
                generateString();
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        };
    }

    @Bean
    public ObservedAspect observedAspect(ObservationRegistry registry) {
        return new ObservedAspect(registry);
    }

    @Observed(name = "server.job", lowCardinalityKeyValues = {"job.type", "string"},
            contextualName = "generate-string")
    private void generateString() throws InterruptedException {
        Thread.sleep(1000);
        log.info("SOMETHING");
    }
}