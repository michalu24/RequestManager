package org.example.requestmanager.infrastructure.transaction;

import org.example.requestmanager.application.port.in.BrowseRequestsQuery;
import org.example.requestmanager.application.port.in.ChangeRequestStateUseCase;
import org.example.requestmanager.application.port.in.CreateRequestUseCase;
import org.example.requestmanager.application.port.in.EditRequestContentUseCase;
import org.example.requestmanager.application.port.in.GetRequestQuery;
import org.example.requestmanager.application.port.in.PageQuery;
import org.example.requestmanager.application.port.in.PageResult;
import org.example.requestmanager.application.port.in.RequestFilter;
import org.example.requestmanager.application.port.in.RequestSummary;
import org.example.requestmanager.domain.model.RequestAction;
import org.example.requestmanager.domain.model.RequestId;
import org.example.requestmanager.domain.model.RequestRoot;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs each use case in a single transaction, so loading, changing and saving a request
 * (and taking a publication number) either all succeed or are rolled back together.
 * Keeps the application core free of Spring transaction annotations.
 */
public class TransactionalRequestService implements
        CreateRequestUseCase,
        EditRequestContentUseCase,
        ChangeRequestStateUseCase,
        GetRequestQuery,
        BrowseRequestsQuery {

    private final CreateRequestUseCase createRequest;
    private final EditRequestContentUseCase editRequestContent;
    private final ChangeRequestStateUseCase changeRequestState;
    private final GetRequestQuery getRequest;
    private final BrowseRequestsQuery browseRequests;
    private final TransactionTemplate writeTransaction;
    private final TransactionTemplate readTransaction;

    public TransactionalRequestService(CreateRequestUseCase createRequest,
                                       EditRequestContentUseCase editRequestContent,
                                       ChangeRequestStateUseCase changeRequestState,
                                       GetRequestQuery getRequest,
                                       BrowseRequestsQuery browseRequests,
                                       PlatformTransactionManager transactionManager) {
        this.createRequest = createRequest;
        this.editRequestContent = editRequestContent;
        this.changeRequestState = changeRequestState;
        this.getRequest = getRequest;
        this.browseRequests = browseRequests;
        this.writeTransaction = new TransactionTemplate(transactionManager);
        this.readTransaction = new TransactionTemplate(transactionManager);
        this.readTransaction.setReadOnly(true);
    }

    @Override
    public RequestRoot create(String name, String content) {
        return writeTransaction.execute(status -> createRequest.create(name, content));
    }

    @Override
    public RequestRoot editContent(RequestId id, String content) {
        return writeTransaction.execute(status -> editRequestContent.editContent(id, content));
    }

    @Override
    public RequestRoot changeState(RequestId id, RequestAction action, String reason) {
        return writeTransaction.execute(status -> changeRequestState.changeState(id, action, reason));
    }

    @Override
    public RequestRoot get(RequestId id) {
        return readTransaction.execute(status -> getRequest.get(id));
    }

    @Override
    public PageResult<RequestSummary> browse(RequestFilter filter, PageQuery page) {
        return readTransaction.execute(status -> browseRequests.browse(filter, page));
    }
}
