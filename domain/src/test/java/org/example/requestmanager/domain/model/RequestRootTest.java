package org.example.requestmanager.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestRootTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    // create

    @Test
    void createsRequestInCreatedState() {
        RequestRoot request = RequestRoot.create("name", "content", NOW);

        assertThat(request.getId()).isNotNull();
        assertThat(request.getName()).isEqualTo("name");
        assertThat(request.getContent()).isEqualTo("content");
        assertThat(request.getState()).isEqualTo(RequestState.CREATED);
        assertThat(request.getPublicationNumber()).isNull();
    }

    @Test
    void recordsCreationInHistory() {
        RequestRoot request = RequestRoot.create("name", "content", NOW);

        assertThat(request.getHistory()).containsExactly(
                new StateChange(null, RequestState.CREATED, RequestAction.CREATE, null, NOW));
    }

    @Test
    void createsRequestsWithDistinctIds() {
        RequestRoot first = RequestRoot.create("name", "content", NOW);
        RequestRoot second = RequestRoot.create("name", "content", NOW);

        assertThat(first.getId()).isNotEqualTo(second.getId());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void rejectsBlankName(String name) {
        assertThatThrownBy(() -> RequestRoot.create(name, "content", NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name must not be blank");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void rejectsBlankContent(String content) {
        assertThatThrownBy(() -> RequestRoot.create("name", content, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("content must not be blank");
    }

    // history

    @Test
    void historyCannotBeModifiedFromOutside() {
        RequestRoot request = RequestRoot.create("name", "content", NOW);

        assertThatThrownBy(() -> request.getHistory().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // stubs - these tests fail on purpose once a method gets implemented

    @Test
    void editContentIsNotImplemented() {
        RequestRoot request = RequestRoot.create("name", "content", NOW);

        assertNotImplemented(() -> request.editContent("new content"));
    }

    @Test
    void verifyIsNotImplemented() {
        RequestRoot request = RequestRoot.create("name", "content", NOW);

        assertNotImplemented(() -> request.verify(NOW));
    }

    @Test
    void acceptIsNotImplemented() {
        RequestRoot request = RequestRoot.create("name", "content", NOW);

        assertNotImplemented(() -> request.accept(NOW));
    }

    @Test
    void publishIsNotImplemented() {
        RequestRoot request = RequestRoot.create("name", "content", NOW);

        assertNotImplemented(() -> request.publish(new PublicationNumber(), NOW));
    }

    @Test
    void deleteIsNotImplemented() {
        RequestRoot request = RequestRoot.create("name", "content", NOW);

        assertNotImplemented(() -> request.delete("reason", NOW));
    }

    @Test
    void rejectIsNotImplemented() {
        RequestRoot request = RequestRoot.create("name", "content", NOW);

        assertNotImplemented(() -> request.reject("reason", NOW));
    }

    private static void assertNotImplemented(Runnable action) {
        assertThatThrownBy(action::run)
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("Not Implemented");
    }
}
