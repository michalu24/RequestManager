package org.example.requestmanager.infrastructure.transaction;

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
import org.example.requestmanager.domain.transition.DiagramTransitionPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionalRequestServiceTest {

    private static final RequestId ID = new RequestId(UUID.randomUUID());
    private static final RequestRoot REQUEST =
            RequestRoot.create("name", "content", new DiagramTransitionPolicy(), Instant.EPOCH);

    private final RecordingTransactionManager transactionManager = new RecordingTransactionManager();

    @Test
    void createRunsInCommittedTransaction() {
        var service = serviceWith((name, content) -> REQUEST, null, null, null);

        assertThat(service.create("name", "content")).isSameAs(REQUEST);
        assertCommittedOnce(false);
    }

    @Test
    void editContentRunsInCommittedTransaction() {
        var service = serviceWith(null, (id, content) -> REQUEST, null, null);

        assertThat(service.editContent(ID, "content")).isSameAs(REQUEST);
        assertCommittedOnce(false);
    }

    @Test
    void changeStateRunsInCommittedTransaction() {
        var service = serviceWith(null, null, (id, action, reason) -> REQUEST, null);

        assertThat(service.changeState(ID, RequestAction.VERIFY, null)).isSameAs(REQUEST);
        assertCommittedOnce(false);
    }

    @Test
    void getRunsInReadOnlyTransaction() {
        var service = serviceWith(null, null, null, id -> REQUEST);

        assertThat(service.get(ID)).isSameAs(REQUEST);
        assertCommittedOnce(true);
    }

    @Test
    void browseRunsInReadOnlyTransaction() {
        PageResult<RequestSummary> page = new PageResult<>(List.of(), 0, 10, 0);
        var service = new TransactionalRequestService(null, null, null, null,
                (filter, query) -> page, transactionManager);

        assertThat(service.browse(new RequestFilter(null, null), new PageQuery(0, 10))).isSameAs(page);
        assertCommittedOnce(true);
    }

    @Test
    void rollsBackAndRethrowsWhenUseCaseFails() {
        var failure = new IllegalStateException("boom");
        var service = serviceWith(null, null, (id, action, reason) -> { throw failure; }, null);

        assertThatThrownBy(() -> service.changeState(ID, RequestAction.VERIFY, null)).isSameAs(failure);
        assertThat(transactionManager.commits).isZero();
        assertThat(transactionManager.rollbacks).isEqualTo(1);
    }

    private TransactionalRequestService serviceWith(CreateRequestUseCase create,
                                                    EditRequestContentUseCase edit,
                                                    ChangeRequestStateUseCase changeState,
                                                    GetRequestQuery get) {
        return new TransactionalRequestService(create, edit, changeState, get, null, transactionManager);
    }

    private void assertCommittedOnce(boolean readOnly) {
        assertThat(transactionManager.commits).isEqualTo(1);
        assertThat(transactionManager.rollbacks).isZero();
        assertThat(transactionManager.lastReadOnly).isEqualTo(readOnly);
    }

    private static class RecordingTransactionManager extends AbstractPlatformTransactionManager {

        int commits;
        int rollbacks;
        boolean lastReadOnly;

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            lastReadOnly = definition.isReadOnly();
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            commits++;
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            rollbacks++;
        }
    }
}
