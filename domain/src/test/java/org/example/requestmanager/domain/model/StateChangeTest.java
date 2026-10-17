package org.example.requestmanager.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StateChangeTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void allowsMissingFromStateAndReason() {
        assertThatNoException().isThrownBy(
                () -> new StateChange(null, RequestState.CREATED, RequestAction.CREATE, null, NOW));
    }

    @Test
    void requiresTargetState() {
        assertThatThrownBy(() -> new StateChange(null, null, RequestAction.CREATE, null, NOW))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void requiresAction() {
        assertThatThrownBy(() -> new StateChange(null, RequestState.CREATED, null, null, NOW))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void requiresTimestamp() {
        assertThatThrownBy(() -> new StateChange(null, RequestState.CREATED, RequestAction.CREATE, null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
