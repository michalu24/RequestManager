package org.example.requestmanager.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.example.requestmanager.domain.model.RequestAction.CREATE;
import static org.example.requestmanager.domain.model.RequestState.CREATED;

public class RequestRoot {

    private final RequestId id;
    private final String name;
    private String content;
    private RequestState state;
    private PublicationNumber publicationNumber;
    private final List<StateChange> history;

    private RequestRoot(RequestId id,
                        String name,
                        String content,
                        RequestState state,
                        PublicationNumber publicationNumber,
                        List<StateChange> history) {
        this.id = id;
        this.name = name;
        this.content = content;
        this.state = state;
        this.publicationNumber = publicationNumber;
        this.history = history;
    }

    public static RequestRoot create(String name, String content, Instant now) {
        requireNotBlank(name, "name");
        requireNotBlank(content, "content");
        return new RequestRoot(
                new RequestId(UUID.randomUUID()),
                name,
                content,
                CREATED,
                null,
                new ArrayList<>(List.of(new StateChange(null, CREATED, CREATE, null, now))));
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
        throw new RuntimeException("Not Implemented");
    }
    public void accept(Instant now) {
        throw new RuntimeException("Not Implemented");
    }
    public void publish(PublicationNumber number, Instant now) {
        throw new RuntimeException("Not Implemented");
    }
    public void delete(String reason, Instant now) {
        throw new RuntimeException("Not Implemented");
    }
    public void reject(String reason, Instant now) {
        throw new RuntimeException("Not Implemented");
    }

    private void transition(RequestAction action, String reason, Instant now) {
        throw new RuntimeException("Not Implemented");
    }
}
