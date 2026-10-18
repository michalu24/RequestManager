package org.example.requestmanager.application.service;

import org.example.requestmanager.application.exception.RequestNotFoundException;
import org.example.requestmanager.domain.exception.InvalidTransitionException;
import org.example.requestmanager.domain.exception.ReasonRequiredException;
import org.example.requestmanager.domain.exception.RequiredFieldMissingException;
import org.example.requestmanager.domain.model.PublicationNumber;
import org.example.requestmanager.domain.model.RequestAction;
import org.example.requestmanager.domain.model.RequestId;
import org.example.requestmanager.domain.model.RequestRoot;
import org.example.requestmanager.domain.model.RequestState;
import org.example.requestmanager.domain.transition.DiagramTransitionPolicy;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final RequestId UNKNOWN_ID = new RequestId(UUID.randomUUID());

    private final InMemoryRequestRepository repository = new InMemoryRequestRepository();
    private final RequestService service = new RequestService(
            repository,
            new SequentialPublicationNumberGenerator(),
            new DiagramTransitionPolicy(),
            Clock.fixed(NOW, ZoneOffset.UTC));

    // create

    @Test
    void createsAndStoresRequest() {
        RequestRoot created = service.create("name", "content");

        RequestRoot stored = repository.findById(created.getId()).orElseThrow();
        assertThat(stored.getName()).isEqualTo("name");
        assertThat(stored.getContent()).isEqualTo("content");
        assertThat(stored.getState()).isEqualTo(RequestState.CREATED);
        assertThat(stored.getHistory()).singleElement()
                .satisfies(change -> assertThat(change.at()).isEqualTo(NOW));
    }

    @Test
    void doesNotStoreInvalidRequest() {
        assertThatThrownBy(() -> service.create(" ", "content"))
                .isInstanceOf(RequiredFieldMissingException.class);
        assertThat(repository.count()).isZero();
    }

    // get

    @Test
    void returnsExistingRequest() {
        RequestRoot created = service.create("name", "content");

        assertThat(service.get(created.getId())).isSameAs(created);
    }

    @Test
    void failsForUnknownRequest() {
        assertNotFound(() -> service.get(UNKNOWN_ID));
    }

    // edit content

    @Test
    void editsContent() {
        RequestRoot created = service.create("name", "content");

        service.editContent(created.getId(), "new content");

        assertThat(repository.findById(created.getId()).orElseThrow().getContent()).isEqualTo("new content");
    }

    @Test
    void editContentFailsForUnknownRequest() {
        assertNotFound(() -> service.editContent(UNKNOWN_ID, "new content"));
    }

    // change state

    @Test
    void verifiesRequest() {
        RequestRoot created = service.create("name", "content");

        RequestRoot result = service.changeState(created.getId(), RequestAction.VERIFY, null);

        assertThat(result.getState()).isEqualTo(RequestState.VERIFIED);
        assertThat(result.getHistory()).last()
                .satisfies(change -> assertThat(change.at()).isEqualTo(NOW));
    }

    @Test
    void publishesRequestsWithUniqueNumbers() {
        RequestRoot first = acceptedRequest();
        RequestRoot second = acceptedRequest();

        service.changeState(first.getId(), RequestAction.PUBLISH, null);
        service.changeState(second.getId(), RequestAction.PUBLISH, null);

        assertThat(first.getState()).isEqualTo(RequestState.PUBLISHED);
        assertThat(first.getPublicationNumber()).isEqualTo(new PublicationNumber(1));
        assertThat(second.getPublicationNumber()).isEqualTo(new PublicationNumber(2));
    }

    @Test
    void deletesRequestWithReason() {
        RequestRoot created = service.create("name", "content");

        service.changeState(created.getId(), RequestAction.DELETE, "duplicate");

        assertThat(created.getState()).isEqualTo(RequestState.DELETED);
        assertThat(created.getHistory()).last()
                .satisfies(change -> assertThat(change.reason()).isEqualTo("duplicate"));
    }

    @Test
    void rejectsRequestWithReason() {
        RequestRoot created = service.create("name", "content");
        service.changeState(created.getId(), RequestAction.VERIFY, null);

        service.changeState(created.getId(), RequestAction.REJECT, "incomplete");

        assertThat(created.getState()).isEqualTo(RequestState.REJECTED);
        assertThat(created.getHistory()).last()
                .satisfies(change -> assertThat(change.reason()).isEqualTo("incomplete"));
    }

    @Test
    void passesDomainErrorsThrough() {
        RequestRoot created = service.create("name", "content");

        assertThatThrownBy(() -> service.changeState(created.getId(), RequestAction.DELETE, null))
                .isInstanceOf(ReasonRequiredException.class);
        assertThatThrownBy(() -> service.changeState(created.getId(), RequestAction.ACCEPT, null))
                .isInstanceOf(InvalidTransitionException.class);
        assertThat(created.getState()).isEqualTo(RequestState.CREATED);
    }

    @Test
    void refusesCreateAsStateChange() {
        RequestRoot created = service.create("name", "content");

        assertThatThrownBy(() -> service.changeState(created.getId(), RequestAction.CREATE, null))
                .isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void changeStateFailsForUnknownRequest() {
        assertNotFound(() -> service.changeState(UNKNOWN_ID, RequestAction.VERIFY, null));
    }

    private RequestRoot acceptedRequest() {
        RequestRoot request = service.create("name", "content");
        service.changeState(request.getId(), RequestAction.VERIFY, null);
        service.changeState(request.getId(), RequestAction.ACCEPT, null);
        return request;
    }

    private static void assertNotFound(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(RequestNotFoundException.class,
                        e -> assertThat(e.getId()).isEqualTo(UNKNOWN_ID));
    }
}
