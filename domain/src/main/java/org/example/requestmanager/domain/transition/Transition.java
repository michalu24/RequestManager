package org.example.requestmanager.domain.transition;

import org.example.requestmanager.domain.model.RequestState;

import java.util.Objects;
import java.util.Set;

public record Transition(Set<RequestState> sources, RequestState target) {

    public Transition {
        sources = Set.copyOf(sources);
        Objects.requireNonNull(target);
    }

    public boolean allowsFrom(RequestState state) {
        return sources.contains(state);
    }
}
