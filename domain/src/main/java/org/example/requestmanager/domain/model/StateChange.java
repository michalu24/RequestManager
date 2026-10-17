package org.example.requestmanager.domain.model;

import java.time.Instant;
import java.util.Objects;

public record StateChange(
        RequestState from,
        RequestState to,
        RequestAction action,
        String reason,
        Instant at) {

    public StateChange {
        Objects.requireNonNull(to);
        Objects.requireNonNull(action);
        Objects.requireNonNull(at);
    }
}