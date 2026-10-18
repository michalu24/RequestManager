package org.example.requestmanager.infrastructure.web.dto;

import org.example.requestmanager.domain.model.RequestAction;

/**
 * {@code reason} is required for DELETE and REJECT and ignored for other actions.
 */
public record TransitionBody(RequestAction action, String reason) {
}
