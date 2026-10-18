package org.example.requestmanager.domain.exception;

import org.example.requestmanager.domain.model.RequestAction;

public class ReasonRequiredException extends RuntimeException {

    private final RequestAction action;

    public ReasonRequiredException(RequestAction action) {
        super("Action " + action + " requires a reason");
        this.action = action;
    }

    public RequestAction getAction() {
        return action;
    }
}
