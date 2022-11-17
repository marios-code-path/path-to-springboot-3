package com.example.observation.aop;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TextPublishingObservationHandler implements ObservationHandler<Observation.Context> {
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
//public class TextPublishingObservationHandler extends ObservationTextPublisher { }