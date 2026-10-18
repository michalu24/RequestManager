package org.example.requestmanager.infrastructure.web;

import org.example.requestmanager.application.port.in.BrowseRequestsQuery;
import org.example.requestmanager.application.port.in.ChangeRequestStateUseCase;
import org.example.requestmanager.application.port.in.CreateRequestUseCase;
import org.example.requestmanager.application.port.in.EditRequestContentUseCase;
import org.example.requestmanager.application.port.in.GetRequestQuery;
import org.example.requestmanager.application.port.in.PageQuery;
import org.example.requestmanager.application.port.in.PageResult;
import org.example.requestmanager.application.port.in.RequestFilter;
import org.example.requestmanager.application.port.in.RequestSummary;
import org.example.requestmanager.domain.model.PublicationNumber;
import org.example.requestmanager.domain.model.RequestAction;
import org.example.requestmanager.domain.model.RequestId;
import org.example.requestmanager.domain.model.RequestRoot;
import org.example.requestmanager.domain.model.RequestState;
import org.example.requestmanager.domain.transition.DiagramTransitionPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RequestController.class)
class RequestControllerTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-01-01T11:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateRequestUseCase createRequest;

    @MockitoBean
    private EditRequestContentUseCase editRequestContent;

    @MockitoBean
    private ChangeRequestStateUseCase changeRequestState;

    @MockitoBean
    private GetRequestQuery getRequest;

    @MockitoBean
    private BrowseRequestsQuery browseRequests;

    @Test
    void browsesWithDefaultPaging() throws Exception {
        RequestSummary summary = new RequestSummary(
                new RequestId(UUID.randomUUID()), "name", RequestState.PUBLISHED, new PublicationNumber(3));
        when(browseRequests.browse(any(), any())).thenReturn(new PageResult<>(List.of(summary), 0, 10, 11));

        mockMvc.perform(get("/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(summary.id().id().toString()))
                .andExpect(jsonPath("$.items[0].name").value("name"))
                .andExpect(jsonPath("$.items[0].state").value("PUBLISHED"))
                .andExpect(jsonPath("$.items[0].publicationNumber").value(3))
                .andExpect(jsonPath("$.items[0].content").doesNotExist())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(11))
                .andExpect(jsonPath("$.totalPages").value(2));

        verify(browseRequests).browse(new RequestFilter(null, null), new PageQuery(0, PageQuery.DEFAULT_SIZE));
    }

    @Test
    void passesFilterAndPagingParameters() throws Exception {
        when(browseRequests.browse(any(), any())).thenReturn(new PageResult<>(List.of(), 2, 5, 0));

        mockMvc.perform(get("/requests")
                        .param("name", "inv")
                        .param("state", "VERIFIED")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(browseRequests).browse(new RequestFilter("inv", RequestState.VERIFIED), new PageQuery(2, 5));
    }

    @Test
    void createsRequest() throws Exception {
        RequestRoot request = newRequest();
        when(createRequest.create("name", "content")).thenReturn(request);

        mockMvc.perform(post("/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "name", "content": "content"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/requests/" + idOf(request))))
                .andExpect(jsonPath("$.id").value(idOf(request)))
                .andExpect(jsonPath("$.name").value("name"))
                .andExpect(jsonPath("$.content").value("content"))
                .andExpect(jsonPath("$.state").value("CREATED"))
                .andExpect(jsonPath("$.publicationNumber").value(nullValue()));
    }

    @Test
    void returnsRequest() throws Exception {
        RequestRoot request = newRequest();
        when(getRequest.get(request.getId())).thenReturn(request);

        mockMvc.perform(get("/requests/{id}", idOf(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(idOf(request)))
                .andExpect(jsonPath("$.state").value("CREATED"));
    }

    @Test
    void returnsPublicationNumberOfPublishedRequest() throws Exception {
        RequestRoot request = newRequest();
        request.verify(LATER);
        request.accept(LATER);
        request.publish(new PublicationNumber(7), LATER);
        when(getRequest.get(request.getId())).thenReturn(request);

        mockMvc.perform(get("/requests/{id}", idOf(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("PUBLISHED"))
                .andExpect(jsonPath("$.publicationNumber").value(7));
    }

    @Test
    void editsContent() throws Exception {
        RequestRoot request = newRequest();
        when(editRequestContent.editContent(any(), any())).thenReturn(request);

        mockMvc.perform(put("/requests/{id}/content", idOf(request))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "new content"}
                                """))
                .andExpect(status().isOk());

        verify(editRequestContent).editContent(request.getId(), "new content");
    }

    @Test
    void changesStateWithActionAndReason() throws Exception {
        RequestRoot request = newRequest();
        when(changeRequestState.changeState(any(), any(), any())).thenReturn(request);

        mockMvc.perform(post("/requests/{id}/transitions", idOf(request))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action": "DELETE", "reason": "duplicate"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(idOf(request)));

        verify(changeRequestState).changeState(request.getId(), RequestAction.DELETE, "duplicate");
    }

    @Test
    void changesStateWithoutReason() throws Exception {
        RequestRoot request = newRequest();
        when(changeRequestState.changeState(any(), any(), any())).thenReturn(request);

        mockMvc.perform(post("/requests/{id}/transitions", idOf(request))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action": "VERIFY"}
                                """))
                .andExpect(status().isOk());

        verify(changeRequestState).changeState(eq(request.getId()), eq(RequestAction.VERIFY), eq(null));
    }

    @Test
    void returnsHistoryInOrder() throws Exception {
        RequestRoot request = newRequest();
        request.verify(LATER);
        request.reject("incomplete", LATER);
        when(getRequest.get(request.getId())).thenReturn(request);

        mockMvc.perform(get("/requests/{id}/history", idOf(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].from").value(nullValue()))
                .andExpect(jsonPath("$[0].to").value("CREATED"))
                .andExpect(jsonPath("$[0].action").value("CREATE"))
                .andExpect(jsonPath("$[0].occurredAt").value("2026-01-01T10:00:00Z"))
                .andExpect(jsonPath("$[1].action").value("VERIFY"))
                .andExpect(jsonPath("$[2].from").value("VERIFIED"))
                .andExpect(jsonPath("$[2].to").value("REJECTED"))
                .andExpect(jsonPath("$[2].reason").value("incomplete"));
    }

    private static RequestRoot newRequest() {
        return RequestRoot.create("name", "content", new DiagramTransitionPolicy(), NOW);
    }

    private static String idOf(RequestRoot request) {
        RequestId id = request.getId();
        return id.id().toString();
    }
}
