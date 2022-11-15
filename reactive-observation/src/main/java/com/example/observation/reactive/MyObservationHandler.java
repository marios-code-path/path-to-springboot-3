package com.example.observation.reactive;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.tracing.handler.TracingAwareMeterObservationHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MyObservationHandler implements ObservationHandler<Observation.Context> {
    @Override
    public void onStart(Observation.Context context) {
        log.info("onStart() ");
    }

    @Override
    public void onStop(Observation.Context context) {
        log.info("onStop() ");
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return true;
    }
}
