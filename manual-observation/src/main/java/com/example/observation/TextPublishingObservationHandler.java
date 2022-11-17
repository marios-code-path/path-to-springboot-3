package com.example.observation;

import io.micrometer.observation.ObservationTextPublisher;
import org.springframework.stereotype.Component;

@Component
public class TextPublishingObservationHandler extends ObservationTextPublisher { }
