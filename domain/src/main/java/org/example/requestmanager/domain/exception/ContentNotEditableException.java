package org.example.requestmanager.domain.exception;

import org.example.requestmanager.domain.model.RequestState;

public class ContentNotEditableException extends RequestDomainException {

    private final RequestState state;

    public ContentNotEditableException(RequestState state) {
        super("Content cannot be edited in state " + state);
        this.state = state;
    }

    public RequestState getState() {
        return state;
    }
}
