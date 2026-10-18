package org.example.requestmanager.application.port.out;

import org.example.requestmanager.domain.model.RequestId;
import org.example.requestmanager.domain.model.RequestRoot;

import java.util.Optional;

public interface RequestRepository {

    RequestRoot save(RequestRoot request);

    Optional<RequestRoot> findById(RequestId id);
}
