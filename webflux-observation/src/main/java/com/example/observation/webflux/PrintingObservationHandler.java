package com.example.observation.webflux;

import io.micrometer.observation.ObservationTextPublisher;
import org.springframework.stereotype.Component;

@Component
class PrintingObservationHandler extends ObservationTextPublisher { }