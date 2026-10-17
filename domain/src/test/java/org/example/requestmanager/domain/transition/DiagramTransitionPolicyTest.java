package org.example.requestmanager.domain.transition;

import org.example.requestmanager.domain.model.RequestAction;
import org.example.requestmanager.domain.model.RequestState;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
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

class DiagramTransitionPolicyTest {

    // Arrows of the state diagram, written out independently of the implementation.
    private static final Map<String, RequestState> DIAGRAM = Map.of(
            key(CREATED, VERIFY),  VERIFIED,
            key(CREATED, DELETE),  DELETED,
            key(VERIFIED, ACCEPT), ACCEPTED,
            key(VERIFIED, REJECT), REJECTED,
            key(ACCEPTED, PUBLISH), PUBLISHED,
            key(ACCEPTED, REJECT), REJECTED);

    private final TransitionPolicy policy = new DiagramTransitionPolicy();

    @ParameterizedTest(name = "{0} --{1}--> {2}")
    @MethodSource("everyStateAndAction")
    void followsTheStateDiagram(RequestState from, RequestAction action, Optional<RequestState> expected) {
        assertThat(policy.targetOf(from, action)).isEqualTo(expected);
    }

    static Stream<Arguments> everyStateAndAction() {
        return Arrays.stream(RequestState.values())
                .flatMap(from -> Arrays.stream(RequestAction.values())
                        .map(action -> Arguments.of(
                                from,
                                action,
                                Optional.ofNullable(DIAGRAM.get(key(from, action))))));
    }

    private static String key(RequestState from, RequestAction action) {
        return from + "/" + action;
    }
}
