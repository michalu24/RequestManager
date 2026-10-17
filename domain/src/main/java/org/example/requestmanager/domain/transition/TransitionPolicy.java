package org.example.requestmanager.domain.transition;

import org.example.requestmanager.domain.model.RequestAction;
import org.example.requestmanager.domain.model.RequestState;

import java.util.Optional;

public interface TransitionPolicy {

    /**
     * Returns the state reached by applying {@code action} in state {@code from},
     * or empty when the transition is not permitted.
     */
    Optional<RequestState> targetOf(RequestState from, RequestAction action);
}
