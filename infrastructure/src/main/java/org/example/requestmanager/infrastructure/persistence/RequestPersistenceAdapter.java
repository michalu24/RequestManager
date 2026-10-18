package org.example.requestmanager.infrastructure.persistence;

import org.example.requestmanager.application.port.out.RequestRepository;
import org.example.requestmanager.domain.model.RequestId;
import org.example.requestmanager.domain.model.RequestRoot;
import org.example.requestmanager.domain.transition.TransitionPolicy;
import org.example.requestmanager.infrastructure.persistence.entity.RequestEntity;
import org.example.requestmanager.infrastructure.persistence.mapper.RequestEntityMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class RequestPersistenceAdapter implements RequestRepository {

    private final RequestJpaRepository jpaRepository;
    private final TransitionPolicy transitionPolicy;

    RequestPersistenceAdapter(RequestJpaRepository jpaRepository, TransitionPolicy transitionPolicy) {
        this.jpaRepository = jpaRepository;
        this.transitionPolicy = transitionPolicy;
    }

    @Override
    @Transactional
    public RequestRoot save(RequestRoot request) {
        RequestEntity entity = jpaRepository.findById(request.getId().id())
                .orElseGet(() -> new RequestEntity(request.getId().id()));
        RequestEntityMapper.updateEntity(entity, request);
        jpaRepository.save(entity);
        return request;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RequestRoot> findById(RequestId id) {
        return jpaRepository.findById(id.id())
                .map(entity -> RequestEntityMapper.toDomain(entity, transitionPolicy));
    }
}
