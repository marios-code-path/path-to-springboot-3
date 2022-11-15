package com.example.observation.reactive;

import io.micrometer.observation.Observation;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.tracing.handler.TracingObservationHandler;
import org.slf4j.MDC;
import reactor.core.publisher.Signal;

import java.util.Optional;
import java.util.function.Consumer;

public class ReactiveLogUtils {
    public static <T> Consumer<Signal<T>> logOnNext(Consumer<T> logStmt) {
        return signal -> {
            if (!signal.isOnNext()) return;
            Optional<Observation> maybeObservation = signal.getContextView().getOrEmpty(ObservationThreadLocalAccessor.KEY);

            maybeObservation.ifPresentOrElse(observation -> {
                try (MDC.MDCCloseable cmdc = MDC.putCloseable("traceId",
                        ((TracingObservationHandler.TracingContext) observation.getContext().get(TracingObservationHandler.TracingContext.class)).getSpan().context().traceId()
                )) {
                    logStmt.accept(signal.get());
                }
            }, () -> {
                logStmt.accept(signal.get());
            });

        };
    }
}
