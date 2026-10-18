package org.example.requestmanager.application.port.in;

public interface BrowseRequestsQuery {

    PageResult<RequestSummary> browse(RequestFilter filter, PageQuery page);
}
