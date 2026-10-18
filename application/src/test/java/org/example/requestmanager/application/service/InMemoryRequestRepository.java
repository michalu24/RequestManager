package org.example.requestmanager.application.service;

import org.example.requestmanager.application.port.in.PageQuery;
import org.example.requestmanager.application.port.in.PageResult;
import org.example.requestmanager.application.port.in.RequestFilter;
import org.example.requestmanager.application.port.in.RequestSummary;
import org.example.requestmanager.application.port.out.RequestRepository;
import org.example.requestmanager.domain.model.RequestId;
import org.example.requestmanager.domain.model.RequestRoot;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

class InMemoryRequestRepository implements RequestRepository {

    private final Map<RequestId, RequestRoot> requests = new HashMap<>();

    @Override
    public RequestRoot save(RequestRoot request) {
        requests.put(request.getId(), request);
        return request;
    }

    @Override
    public Optional<RequestRoot> findById(RequestId id) {
        return Optional.ofNullable(requests.get(id));
    }

    @Override
    public PageResult<RequestSummary> search(RequestFilter filter, PageQuery page) {
        List<RequestSummary> matching = requests.values().stream()
                .filter(request -> filter.name() == null || request.getName().toLowerCase(Locale.ROOT)
                        .contains(filter.name().toLowerCase(Locale.ROOT)))
                .filter(request -> filter.state() == null || request.getState() == filter.state())
                .sorted(Comparator.comparing(RequestRoot::getName)
                        .thenComparing(request -> request.getId().id()))
                .map(request -> new RequestSummary(
                        request.getId(), request.getName(), request.getState(), request.getPublicationNumber()))
                .toList();
        List<RequestSummary> items = matching.stream()
                .skip((long) page.page() * page.size())
                .limit(page.size())
                .toList();
        return new PageResult<>(items, page.page(), page.size(), matching.size());
    }

    int count() {
        return requests.size();
    }
}
