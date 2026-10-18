package org.example.requestmanager.infrastructure.web.dto;

import org.example.requestmanager.application.port.in.RequestSummary;
import org.example.requestmanager.domain.model.RequestState;

import java.util.UUID;

public record RequestSummaryResponse(
        UUID id,
        String name,
        RequestState state,
        Long publicationNumber) {

    public static RequestSummaryResponse from(RequestSummary summary) {
        return new RequestSummaryResponse(
                summary.id().id(),
                summary.name(),
                summary.state(),
                summary.publicationNumber() == null ? null : summary.publicationNumber().value());
    }
}
