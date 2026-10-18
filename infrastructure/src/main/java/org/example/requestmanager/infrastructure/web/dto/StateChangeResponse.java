package org.example.requestmanager.infrastructure.web.dto;

import org.example.requestmanager.domain.model.RequestAction;
import org.example.requestmanager.domain.model.RequestState;
import org.example.requestmanager.domain.model.StateChange;

import java.time.Instant;

public record StateChangeResponse(
        RequestState from,
        RequestState to,
        RequestAction action,
        String reason,
        Instant occurredAt) {

    public static StateChangeResponse from(StateChange change) {
        return new StateChangeResponse(
                change.from(),
                change.to(),
                change.action(),
                change.reason(),
                change.at());
    }
}
