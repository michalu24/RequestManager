package org.example.requestmanager.infrastructure;

import org.example.requestmanager.application.exception.RequestNotFoundException;
import org.example.requestmanager.application.port.in.ChangeRequestStateUseCase;
import org.example.requestmanager.application.port.in.CreateRequestUseCase;
import org.example.requestmanager.application.port.in.EditRequestContentUseCase;
import org.example.requestmanager.application.port.in.GetRequestQuery;
import org.example.requestmanager.domain.exception.ReasonRequiredException;
import org.example.requestmanager.domain.model.RequestAction;
import org.example.requestmanager.domain.model.RequestId;
import org.example.requestmanager.domain.model.RequestRoot;
import org.example.requestmanager.domain.model.RequestState;
import org.example.requestmanager.infrastructure.transaction.TransactionalRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RequestUseCasesIntegrationTest {

    @Autowired
    private CreateRequestUseCase createRequest;

    @Autowired
    private EditRequestContentUseCase editRequestContent;

    @Autowired
    private ChangeRequestStateUseCase changeRequestState;

    @Autowired
    private GetRequestQuery getRequest;

    @Autowired
    private ApplicationContext context;

    @Test
    void everyUseCasePortIsServedByTheTransactionalDecorator() {
        for (Class<?> port : new Class<?>[]{CreateRequestUseCase.class, EditRequestContentUseCase.class,
                ChangeRequestStateUseCase.class, GetRequestQuery.class}) {
            assertThat(context.getBeansOfType(port).values())
                    .singleElement()
                    .isInstanceOf(TransactionalRequestService.class);
        }
    }

    @Test
    void publishesRequestThroughTheWholeStack() {
        RequestId id = createRequest.create("name", "content").getId();
        editRequestContent.editContent(id, "edited");
        changeRequestState.changeState(id, RequestAction.VERIFY, null);
        changeRequestState.changeState(id, RequestAction.ACCEPT, null);
        changeRequestState.changeState(id, RequestAction.PUBLISH, null);

        RequestRoot stored = getRequest.get(id);
        assertThat(stored.getContent()).isEqualTo("edited");
        assertThat(stored.getState()).isEqualTo(RequestState.PUBLISHED);
        assertThat(stored.getPublicationNumber()).isNotNull();
        assertThat(stored.getHistory())
                .extracting(change -> change.action())
                .containsExactly(RequestAction.CREATE, RequestAction.VERIFY, RequestAction.ACCEPT, RequestAction.PUBLISH);
    }

    @Test
    void failedUseCaseLeavesStoredRequestUnchanged() {
        RequestId id = createRequest.create("name", "content").getId();

        assertThatThrownBy(() -> changeRequestState.changeState(id, RequestAction.DELETE, " "))
                .isInstanceOf(ReasonRequiredException.class);

        RequestRoot stored = getRequest.get(id);
        assertThat(stored.getState()).isEqualTo(RequestState.CREATED);
        assertThat(stored.getHistory()).hasSize(1);
    }

    @Test
    void unknownRequestIsReportedAsNotFound() {
        assertThatThrownBy(() -> getRequest.get(new RequestId(UUID.randomUUID())))
                .isInstanceOf(RequestNotFoundException.class);
    }
}
