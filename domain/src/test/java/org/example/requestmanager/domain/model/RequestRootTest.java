package org.example.requestmanager.domain.model;

import org.example.requestmanager.domain.exception.InvalidTransitionException;
import org.example.requestmanager.domain.transition.DiagramTransitionPolicy;
import org.example.requestmanager.domain.transition.TransitionPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestRootTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-01-01T11:00:00Z");
    private static final TransitionPolicy POLICY = new DiagramTransitionPolicy();

    // create

    @Test
    void createsRequestInCreatedState() {
        RequestRoot request = newRequest();

        assertThat(request.getId()).isNotNull();
        assertThat(request.getName()).isEqualTo("name");
        assertThat(request.getContent()).isEqualTo("content");
        assertThat(request.getState()).isEqualTo(RequestState.CREATED);
        assertThat(request.getPublicationNumber()).isNull();
    }

    @Test
    void recordsCreationInHistory() {
        RequestRoot request = newRequest();

        assertThat(request.getHistory()).containsExactly(
                new StateChange(null, RequestState.CREATED, RequestAction.CREATE, null, NOW));
    }

    @Test
    void createsRequestsWithDistinctIds() {
        RequestRoot first = newRequest();
        RequestRoot second = newRequest();

        assertThat(first.getId()).isNotEqualTo(second.getId());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void rejectsBlankName(String name) {
        assertThatThrownBy(() -> RequestRoot.create(name, "content", POLICY, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name must not be blank");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void rejectsBlankContent(String content) {
        assertThatThrownBy(() -> RequestRoot.create("name", content, POLICY, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("content must not be blank");
    }

    @Test
    void requiresTransitionPolicy() {
        assertThatThrownBy(() -> RequestRoot.create("name", "content", null, NOW))
                .isInstanceOf(NullPointerException.class);
    }

    // history

    @Test
    void historyCannotBeModifiedFromOutside() {
        RequestRoot request = newRequest();

        assertThatThrownBy(() -> request.getHistory().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // verify

    @Test
    void verifyMovesCreatedRequestToVerified() {
        RequestRoot request = newRequest();

        request.verify(LATER);

        assertThat(request.getState()).isEqualTo(RequestState.VERIFIED);
        assertThat(request.getHistory()).last().isEqualTo(
                new StateChange(RequestState.CREATED, RequestState.VERIFIED, RequestAction.VERIFY, null, LATER));
    }

    @Test
    void verifyIsRejectedOutsideCreatedState() {
        RequestRoot request = newRequest();
        request.verify(NOW);

        assertInvalidTransition(request, () -> request.verify(LATER), RequestState.VERIFIED, RequestAction.VERIFY);
    }

    // accept

    @Test
    void acceptMovesVerifiedRequestToAccepted() {
        RequestRoot request = newRequest();
        request.verify(NOW);

        request.accept(LATER);

        assertThat(request.getState()).isEqualTo(RequestState.ACCEPTED);
        assertThat(request.getHistory()).last().isEqualTo(
                new StateChange(RequestState.VERIFIED, RequestState.ACCEPTED, RequestAction.ACCEPT, null, LATER));
    }

    @Test
    void acceptIsRejectedOutsideVerifiedState() {
        RequestRoot request = newRequest();

        assertInvalidTransition(request, () -> request.accept(LATER), RequestState.CREATED, RequestAction.ACCEPT);
    }

    // policy

    @Test
    void transitionsAreDecidedByTheGivenPolicy() {
        TransitionPolicy nothingAllowed = (from, action) -> Optional.empty();
        RequestRoot request = RequestRoot.create("name", "content", nothingAllowed, NOW);

        assertInvalidTransition(request, () -> request.verify(LATER), RequestState.CREATED, RequestAction.VERIFY);
    }

    // stubs - these tests fail on purpose once a method gets implemented

    @Test
    void editContentIsNotImplemented() {
        RequestRoot request = newRequest();

        assertNotImplemented(() -> request.editContent("new content"));
    }

    @Test
    void publishIsNotImplemented() {
        RequestRoot request = newRequest();

        assertNotImplemented(() -> request.publish(new PublicationNumber(), NOW));
    }

    @Test
    void deleteIsNotImplemented() {
        RequestRoot request = newRequest();

        assertNotImplemented(() -> request.delete("reason", NOW));
    }

    @Test
    void rejectIsNotImplemented() {
        RequestRoot request = newRequest();

        assertNotImplemented(() -> request.reject("reason", NOW));
    }

    private static RequestRoot newRequest() {
        return RequestRoot.create("name", "content", POLICY, NOW);
    }

    private static void assertInvalidTransition(RequestRoot request,
                                                Runnable action,
                                                RequestState expectedFrom,
                                                RequestAction expectedAction) {
        var historyBefore = request.getHistory();

        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(InvalidTransitionException.class, e -> {
                    assertThat(e.getFrom()).isEqualTo(expectedFrom);
                    assertThat(e.getAction()).isEqualTo(expectedAction);
                });
        assertThat(request.getState()).isEqualTo(expectedFrom);
        assertThat(request.getHistory()).isEqualTo(historyBefore);
    }

    private static void assertNotImplemented(Runnable action) {
        assertThatThrownBy(action::run)
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("Not Implemented");
    }
}
