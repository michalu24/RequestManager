package org.example.requestmanager.application.port.out;

import org.example.requestmanager.application.port.in.PageQuery;
import org.example.requestmanager.application.port.in.PageResult;
import org.example.requestmanager.application.port.in.RequestFilter;
import org.example.requestmanager.application.port.in.RequestSummary;
import org.example.requestmanager.domain.model.RequestId;
import org.example.requestmanager.domain.model.RequestRoot;

import java.util.Optional;

public interface RequestRepository {

    RequestRoot save(RequestRoot request);

    Optional<RequestRoot> findById(RequestId id);

    /**
     * Returns matching requests ordered by name, then id.
     */
    PageResult<RequestSummary> search(RequestFilter filter, PageQuery page);
}
