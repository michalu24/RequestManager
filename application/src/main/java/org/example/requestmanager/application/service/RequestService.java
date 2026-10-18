package org.example.requestmanager.application.service;

import org.example.requestmanager.application.exception.RequestNotFoundException;
import org.example.requestmanager.application.port.in.BrowseRequestsQuery;
import org.example.requestmanager.application.port.in.ChangeRequestStateUseCase;
import org.example.requestmanager.application.port.in.CreateRequestUseCase;
import org.example.requestmanager.application.port.in.EditRequestContentUseCase;
import org.example.requestmanager.application.port.in.GetRequestQuery;
import org.example.requestmanager.application.port.in.PageQuery;
import org.example.requestmanager.application.port.in.PageResult;
import org.example.requestmanager.application.port.in.RequestFilter;
import org.example.requestmanager.application.port.in.RequestSummary;
import org.example.requestmanager.application.port.out.PublicationNumberGenerator;
import org.example.requestmanager.application.port.out.RequestRepository;
import org.example.requestmanager.domain.exception.InvalidTransitionException;
import org.example.requestmanager.domain.model.RequestAction;
import org.example.requestmanager.domain.model.RequestId;
import org.example.requestmanager.domain.model.RequestRoot;
import org.example.requestmanager.domain.transition.TransitionPolicy;

import java.time.Clock;
import java.time.Instant;

public class RequestService implements
        CreateRequestUseCase,
        EditRequestContentUseCase,
        ChangeRequestStateUseCase,
        GetRequestQuery,
        BrowseRequestsQuery {

    private final RequestRepository repository;
    private final PublicationNumberGenerator publicationNumberGenerator;
    private final TransitionPolicy transitionPolicy;
    private final Clock clock;

    public RequestService(RequestRepository repository,
                          PublicationNumberGenerator publicationNumberGenerator,
                          TransitionPolicy transitionPolicy,
                          Clock clock) {
        this.repository = repository;
        this.publicationNumberGenerator = publicationNumberGenerator;
        this.transitionPolicy = transitionPolicy;
        this.clock = clock;
    }

    @Override
    public RequestRoot create(String name, String content) {
        RequestRoot request = RequestRoot.create(name, content, transitionPolicy, clock.instant());
        return repository.save(request);
    }

    @Override
    public RequestRoot editContent(RequestId id, String content) {
        RequestRoot request = load(id);
        request.editContent(content);
        return repository.save(request);
    }

    @Override
    public RequestRoot changeState(RequestId id, RequestAction action, String reason) {
        RequestRoot request = load(id);
        Instant now = clock.instant();
        switch (action) {
            case VERIFY -> request.verify(now);
            case ACCEPT -> request.accept(now);
            case PUBLISH -> request.publish(publicationNumberGenerator.next(), now);
            case DELETE -> request.delete(reason, now);
            case REJECT -> request.reject(reason, now);
            case CREATE -> throw new InvalidTransitionException(request.getState(), action);
        }
        return repository.save(request);
    }

    @Override
    public RequestRoot get(RequestId id) {
        return load(id);
    }

    @Override
    public PageResult<RequestSummary> browse(RequestFilter filter, PageQuery page) {
        return repository.search(filter, page);
    }

    private RequestRoot load(RequestId id) {
        return repository.findById(id)
                .orElseThrow(() -> new RequestNotFoundException(id));
    }
}
