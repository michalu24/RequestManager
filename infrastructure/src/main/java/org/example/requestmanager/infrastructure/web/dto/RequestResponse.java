package org.example.requestmanager.infrastructure.web.dto;

import org.example.requestmanager.domain.model.RequestRoot;
import org.example.requestmanager.domain.model.RequestState;

import java.util.UUID;

public record RequestResponse(
        UUID id,
        String name,
        String content,
        RequestState state,
        Long publicationNumber) {

    public static RequestResponse from(RequestRoot request) {
        return new RequestResponse(
                request.getId().id(),
                request.getName(),
                request.getContent(),
                request.getState(),
                request.getPublicationNumber() == null ? null : request.getPublicationNumber().value());
    }
}
