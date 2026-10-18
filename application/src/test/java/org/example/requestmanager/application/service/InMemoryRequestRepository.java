package org.example.requestmanager.application.service;

import org.example.requestmanager.application.port.out.RequestRepository;
import org.example.requestmanager.domain.model.RequestId;
import org.example.requestmanager.domain.model.RequestRoot;

import java.util.HashMap;
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

    int count() {
        return requests.size();
    }
}
