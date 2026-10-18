package org.example.requestmanager.infrastructure.persistence.mapper;

import org.example.requestmanager.domain.model.PublicationNumber;
import org.example.requestmanager.domain.model.RequestId;
import org.example.requestmanager.domain.model.RequestRoot;
import org.example.requestmanager.domain.model.StateChange;
import org.example.requestmanager.domain.transition.TransitionPolicy;
import org.example.requestmanager.infrastructure.persistence.entity.RequestEntity;
import org.example.requestmanager.infrastructure.persistence.entity.StateChangeEntity;

import java.util.List;

public final class RequestEntityMapper {

    private RequestEntityMapper() {
    }

    public static RequestRoot toDomain(RequestEntity entity, TransitionPolicy transitionPolicy) {
        List<StateChange> history = entity.getHistory().stream()
                .map(RequestEntityMapper::toDomain)
                .toList();
        PublicationNumber publicationNumber = entity.getPublicationNumber() == null
                ? null
                : new PublicationNumber(entity.getPublicationNumber());
        return RequestRoot.restore(
                new RequestId(entity.getId()),
                entity.getName(),
                entity.getContent(),
                entity.getState(),
                publicationNumber,
                history,
                transitionPolicy);
    }

    /**
     * Copies the request into the entity. History is append-only, so only entries
     * not yet stored are added; stored entries are never rewritten.
     */
    public static void updateEntity(RequestEntity entity, RequestRoot request) {
        entity.setName(request.getName());
        entity.setContent(request.getContent());
        entity.setState(request.getState());
        entity.setPublicationNumber(request.getPublicationNumber() == null
                ? null
                : request.getPublicationNumber().value());

        List<StateChange> history = request.getHistory();
        history.subList(entity.getHistory().size(), history.size()).stream()
                .map(RequestEntityMapper::toEntity)
                .forEach(entity.getHistory()::add);
    }

    private static StateChange toDomain(StateChangeEntity entity) {
        return new StateChange(
                entity.getFromState(),
                entity.getToState(),
                entity.getAction(),
                entity.getReason(),
                entity.getOccurredAt());
    }

    private static StateChangeEntity toEntity(StateChange change) {
        return new StateChangeEntity(
                change.from(),
                change.to(),
                change.action(),
                change.reason(),
                change.at());
    }
}
