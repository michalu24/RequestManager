package org.example.requestmanager.domain.exception;

import org.example.requestmanager.domain.model.RequestAction;
import org.example.requestmanager.domain.model.RequestState;

public class  InvalidTransitionException extends RuntimeException {

    private final RequestState from;
    private final RequestAction action;

    public InvalidTransitionException(RequestState from, RequestAction action) {
        super("Action " + action + " is not allowed in state " + from);
        this.from = from;
        this.action = action;
    }

    public RequestState getFrom() {
        return from;
    }

    public RequestAction getAction() {
        return action;
    }
}
