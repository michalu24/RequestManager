package org.example.requestmanager.domain.transition;

import org.example.requestmanager.domain.model.RequestAction;
import org.example.requestmanager.domain.model.RequestState;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.example.requestmanager.domain.model.RequestAction.ACCEPT;
import static org.example.requestmanager.domain.model.RequestAction.DELETE;
import static org.example.requestmanager.domain.model.RequestAction.PUBLISH;
import static org.example.requestmanager.domain.model.RequestAction.REJECT;
import static org.example.requestmanager.domain.model.RequestAction.VERIFY;
import static org.example.requestmanager.domain.model.RequestState.ACCEPTED;
import static org.example.requestmanager.domain.model.RequestState.CREATED;
import static org.example.requestmanager.domain.model.RequestState.DELETED;
import static org.example.requestmanager.domain.model.RequestState.PUBLISHED;
import static org.example.requestmanager.domain.model.RequestState.REJECTED;
import static org.example.requestmanager.domain.model.RequestState.VERIFIED;

public final class DiagramTransitionPolicy implements TransitionPolicy {

    private static final Map<RequestAction, Transition> TRANSITIONS = Map.of(
            VERIFY, new Transition(Set.of(CREATED), VERIFIED),
            ACCEPT, new Transition(Set.of(VERIFIED), ACCEPTED),
            PUBLISH, new Transition(Set.of(ACCEPTED), PUBLISHED),
            DELETE, new Transition(Set.of(CREATED), DELETED),
            REJECT, new Transition(Set.of(VERIFIED, ACCEPTED), REJECTED));

    @Override
    public Optional<RequestState> targetOf(RequestState from, RequestAction action) {
        return Optional.ofNullable(TRANSITIONS.get(action))
                .filter(transition -> transition.allowsFrom(from))
                .map(Transition::target);
    }
}
