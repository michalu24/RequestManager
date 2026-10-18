package org.example.requestmanager.infrastructure.persistence;

import org.example.requestmanager.application.port.in.PageQuery;
import org.example.requestmanager.application.port.in.PageResult;
import org.example.requestmanager.application.port.in.RequestFilter;
import org.example.requestmanager.application.port.in.RequestSummary;
import org.example.requestmanager.application.port.out.RequestRepository;
import org.example.requestmanager.domain.model.PublicationNumber;
import org.example.requestmanager.domain.model.RequestId;
import org.example.requestmanager.domain.model.RequestRoot;
import org.example.requestmanager.domain.transition.TransitionPolicy;
import org.example.requestmanager.infrastructure.persistence.entity.RequestEntity;
import org.example.requestmanager.infrastructure.persistence.mapper.RequestEntityMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
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

    @Override
    @Transactional(readOnly = true)
    public PageResult<RequestSummary> search(RequestFilter filter, PageQuery page) {
        Page<RequestSummaryRow> rows = jpaRepository.search(
                namePattern(filter.name()),
                filter.state(),
                PageRequest.of(page.page(), page.size(), Sort.by("name", "id")));
        List<RequestSummary> items = rows.stream()
                .map(RequestPersistenceAdapter::toSummary)
                .toList();
        return new PageResult<>(items, page.page(), page.size(), rows.getTotalElements());
    }

    /**
     * Matches any part of the name, ignoring case. LIKE wildcards typed by the user are
     * escaped so they match literally; no filter matches every name.
     */
    private static String namePattern(String name) {
        if (name == null) {
            return "%";
        }
        String escaped = name.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private static RequestSummary toSummary(RequestSummaryRow row) {
        return new RequestSummary(
                new RequestId(row.getId()),
                row.getName(),
                row.getState(),
                row.getPublicationNumber() == null ? null : new PublicationNumber(row.getPublicationNumber()));
    }
}
