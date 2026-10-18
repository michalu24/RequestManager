package org.example.requestmanager.application.exception;

import org.example.requestmanager.domain.model.RequestId;

public class RequestNotFoundException extends RuntimeException {

    private final RequestId id;

    public RequestNotFoundException(RequestId id) {
        super("Request " + id.id() + " not found");
        this.id = id;
    }

    public RequestId getId() {
        return id;
    }
}
