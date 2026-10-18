package org.example.requestmanager.domain.model;

import org.example.requestmanager.domain.exception.ContentNotEditableException;
import org.example.requestmanager.domain.exception.InvalidTransitionException;
import org.example.requestmanager.domain.exception.ReasonRequiredException;
import org.example.requestmanager.domain.exception.RequiredFieldMissingException;
import org.example.requestmanager.domain.transition.DiagramTransitionPolicy;
import org.example.requestmanager.domain.transition.TransitionPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
        assertRequiredFieldMissing(() -> RequestRoot.create(name, "content", POLICY, NOW), "name");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void rejectsBlankContent(String content) {
        assertRequiredFieldMissing(() -> RequestRoot.create("name", content, POLICY, NOW), "content");
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

    // delete

    @Test
    void deleteMovesCreatedRequestToDeletedWithReason() {
        RequestRoot request = newRequest();

        request.delete("duplicate", LATER);

        assertThat(request.getState()).isEqualTo(RequestState.DELETED);
        assertThat(request.getHistory()).last().isEqualTo(
                new StateChange(RequestState.CREATED, RequestState.DELETED, RequestAction.DELETE, "duplicate", LATER));
    }

    @Test
    void deleteIsRejectedOutsideCreatedState() {
        RequestRoot request = newRequest();
        request.verify(NOW);

        assertInvalidTransition(request, () -> request.delete("duplicate", LATER), RequestState.VERIFIED, RequestAction.DELETE);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void deleteRequiresReason(String reason) {
        RequestRoot request = newRequest();

        assertReasonRequired(request, () -> request.delete(reason, LATER), RequestAction.DELETE);
    }

    // reject

    @Test
    void rejectMovesVerifiedRequestToRejectedWithReason() {
        RequestRoot request = newRequest();
        request.verify(NOW);

        request.reject("incomplete", LATER);

        assertThat(request.getState()).isEqualTo(RequestState.REJECTED);
        assertThat(request.getHistory()).last().isEqualTo(
                new StateChange(RequestState.VERIFIED, RequestState.REJECTED, RequestAction.REJECT, "incomplete", LATER));
    }

    @Test
    void rejectMovesAcceptedRequestToRejectedWithReason() {
        RequestRoot request = newRequest();
        request.verify(NOW);
        request.accept(NOW);

        request.reject("incomplete", LATER);

        assertThat(request.getState()).isEqualTo(RequestState.REJECTED);
        assertThat(request.getHistory()).last().isEqualTo(
                new StateChange(RequestState.ACCEPTED, RequestState.REJECTED, RequestAction.REJECT, "incomplete", LATER));
    }

    @Test
    void rejectIsRejectedInCreatedState() {
        RequestRoot request = newRequest();

        assertInvalidTransition(request, () -> request.reject("incomplete", LATER), RequestState.CREATED, RequestAction.REJECT);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void rejectRequiresReason(String reason) {
        RequestRoot request = newRequest();
        request.verify(NOW);

        assertReasonRequired(request, () -> request.reject(reason, LATER), RequestAction.REJECT);
    }

    // publish

    @Test
    void publishMovesAcceptedRequestToPublishedWithNumber() {
        RequestRoot request = newRequest();
        request.verify(NOW);
        request.accept(NOW);

        request.publish(new PublicationNumber(42), LATER);

        assertThat(request.getState()).isEqualTo(RequestState.PUBLISHED);
        assertThat(request.getPublicationNumber()).isEqualTo(new PublicationNumber(42));
        assertThat(request.getHistory()).last().isEqualTo(
                new StateChange(RequestState.ACCEPTED, RequestState.PUBLISHED, RequestAction.PUBLISH, null, LATER));
    }

    @Test
    void publishIsRejectedOutsideAcceptedStateAndAssignsNoNumber() {
        RequestRoot request = newRequest();
        request.verify(NOW);

        assertInvalidTransition(request, () -> request.publish(new PublicationNumber(42), LATER),
                RequestState.VERIFIED, RequestAction.PUBLISH);
        assertThat(request.getPublicationNumber()).isNull();
    }

    @Test
    void publishRequiresNumber() {
        RequestRoot request = newRequest();
        request.verify(NOW);
        request.accept(NOW);
        var historyBefore = request.getHistory();

        assertThatThrownBy(() -> request.publish(null, LATER))
                .isInstanceOf(NullPointerException.class);
        assertThat(request.getState()).isEqualTo(RequestState.ACCEPTED);
        assertThat(request.getHistory()).isEqualTo(historyBefore);
    }

    // policy

    @Test
    void transitionsAreDecidedByTheGivenPolicy() {
        TransitionPolicy nothingAllowed = (from, action) -> Optional.empty();
        RequestRoot request = RequestRoot.create("name", "content", nothingAllowed, NOW);

        assertInvalidTransition(request, () -> request.verify(LATER), RequestState.CREATED, RequestAction.VERIFY);
    }

    // edit content

    @ParameterizedTest
    @EnumSource(value = RequestState.class, names = {"CREATED", "VERIFIED"})
    void editsContentInEditableState(RequestState state) {
        RequestRoot request = requestIn(state);
        var historyBefore = request.getHistory();

        request.editContent("new content");

        assertThat(request.getContent()).isEqualTo("new content");
        assertThat(request.getState()).isEqualTo(state);
        assertThat(request.getHistory()).isEqualTo(historyBefore);
    }

    @ParameterizedTest
    @EnumSource(value = RequestState.class, names = {"ACCEPTED", "PUBLISHED", "DELETED", "REJECTED"})
    void refusesToEditContentInOtherStates(RequestState state) {
        RequestRoot request = requestIn(state);

        assertThatThrownBy(() -> request.editContent("new content"))
                .isInstanceOfSatisfying(ContentNotEditableException.class,
                        e -> assertThat(e.getState()).isEqualTo(state));
        assertThat(request.getContent()).isEqualTo("content");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void refusesBlankContentOnEdit(String content) {
        RequestRoot request = newRequest();

        assertRequiredFieldMissing(() -> request.editContent(content), "content");
        assertThat(request.getContent()).isEqualTo("content");
    }

    private static RequestRoot newRequest() {
        return RequestRoot.create("name", "content", POLICY, NOW);
    }

    private static RequestRoot requestIn(RequestState state) {
        RequestRoot request = newRequest();
        switch (state) {
            case CREATED -> { }
            case VERIFIED -> request.verify(NOW);
            case ACCEPTED -> {
                request.verify(NOW);
                request.accept(NOW);
            }
            case PUBLISHED -> {
                request.verify(NOW);
                request.accept(NOW);
                request.publish(new PublicationNumber(1), NOW);
            }
            case DELETED -> request.delete("reason", NOW);
            case REJECTED -> {
                request.verify(NOW);
                request.reject("reason", NOW);
            }
        }
        return request;
    }

    private static void assertRequiredFieldMissing(Runnable action, String expectedField) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(RequiredFieldMissingException.class,
                        e -> assertThat(e.getField()).isEqualTo(expectedField))
                .hasMessage(expectedField + " must not be blank");
    }

    private static void assertReasonRequired(RequestRoot request, Runnable action, RequestAction expectedAction) {
        RequestState stateBefore = request.getState();
        var historyBefore = request.getHistory();

        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(ReasonRequiredException.class,
                        e -> assertThat(e.getAction()).isEqualTo(expectedAction));
        assertThat(request.getState()).isEqualTo(stateBefore);
        assertThat(request.getHistory()).isEqualTo(historyBefore);
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
}
