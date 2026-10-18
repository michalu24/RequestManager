package org.example.requestmanager.domain.model;

import org.example.requestmanager.domain.exception.InvalidTransitionException;
import org.example.requestmanager.domain.exception.ReasonRequiredException;
import org.example.requestmanager.domain.transition.TransitionPolicy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.example.requestmanager.domain.model.RequestAction.ACCEPT;
import static org.example.requestmanager.domain.model.RequestAction.CREATE;
import static org.example.requestmanager.domain.model.RequestAction.DELETE;
import static org.example.requestmanager.domain.model.RequestAction.PUBLISH;
import static org.example.requestmanager.domain.model.RequestAction.REJECT;
import static org.example.requestmanager.domain.model.RequestAction.VERIFY;
import static org.example.requestmanager.domain.model.RequestState.CREATED;

public class RequestRoot {

    private final RequestId id;
    private final String name;
    private String content;
    private RequestState state;
    private PublicationNumber publicationNumber;
    private final List<StateChange> history;
    private final TransitionPolicy transitionPolicy;

    private RequestRoot(RequestId id,
                        String name,
                        String content,
                        RequestState state,
                        PublicationNumber publicationNumber,
                        List<StateChange> history,
                        TransitionPolicy transitionPolicy) {
        this.id = id;
        this.name = name;
        this.content = content;
        this.state = state;
        this.publicationNumber = publicationNumber;
        this.history = history;
        this.transitionPolicy = transitionPolicy;
    }

    public static RequestRoot create(String name, String content, TransitionPolicy transitionPolicy, Instant now) {
        requireNotBlank(name, "name");
        requireNotBlank(content, "content");
        Objects.requireNonNull(transitionPolicy);
        return new RequestRoot(
                new RequestId(UUID.randomUUID()),
                name,
                content,
                CREATED,
                null,
                new ArrayList<>(List.of(new StateChange(null, CREATED, CREATE, null, now))),
                transitionPolicy);
    }
    public RequestId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }

    public RequestState getState() {
        return state;
    }

    public PublicationNumber getPublicationNumber() {
        return publicationNumber;
    }

    public List<StateChange> getHistory() {
        return List.copyOf(history);
    }

    private static void requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }

    public void editContent(String content) {
        throw new RuntimeException("Not Implemented");
    }

    public void verify(Instant now) {
        transition(VERIFY, null, now);
    }

    public void accept(Instant now) {
        transition(ACCEPT, null, now);
    }

    public void publish(PublicationNumber number, Instant now) {
        Objects.requireNonNull(number);
        transition(PUBLISH, null, now);
        publicationNumber = number;
    }

    public void delete(String reason, Instant now) {
        requireReason(reason, DELETE);
        transition(DELETE, reason, now);
    }

    public void reject(String reason, Instant now) {
        requireReason(reason, REJECT);
        transition(REJECT, reason, now);
    }

    private static void requireReason(String reason, RequestAction action) {
        if (reason == null || reason.isBlank()) {
            throw new ReasonRequiredException(action);
        }
    }

    private void transition(RequestAction action, String reason, Instant now) {
        RequestState target = transitionPolicy.targetOf(state, action)
                .orElseThrow(() -> new InvalidTransitionException(state, action));
        history.add(new StateChange(state, target, action, reason, now));
        state = target;
    }
}
