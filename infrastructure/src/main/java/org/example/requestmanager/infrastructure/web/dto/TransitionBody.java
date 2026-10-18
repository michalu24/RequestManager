package org.example.requestmanager.infrastructure.web.dto;

import org.example.requestmanager.domain.model.RequestAction;

/**
 * {@code reason} is required for DELETE and REJECT and ignored for other actions.
 * A missing {@code action} fails JSON parsing, which results in 400 Bad Request.
 */
public record TransitionBody(RequestAction action, String reason) {

    public TransitionBody {
        if (action == null) {
            throw new IllegalArgumentException("action must not be null");
        }
    }
}
