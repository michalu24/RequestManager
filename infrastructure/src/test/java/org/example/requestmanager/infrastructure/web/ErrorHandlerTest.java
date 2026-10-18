package org.example.requestmanager.infrastructure.web;

import org.example.requestmanager.application.exception.RequestNotFoundException;
import org.example.requestmanager.application.port.in.BrowseRequestsQuery;
import org.example.requestmanager.application.port.in.ChangeRequestStateUseCase;
import org.example.requestmanager.application.port.in.CreateRequestUseCase;
import org.example.requestmanager.application.port.in.EditRequestContentUseCase;
import org.example.requestmanager.application.port.in.GetRequestQuery;
import org.example.requestmanager.domain.exception.ContentNotEditableException;
import org.example.requestmanager.domain.exception.FieldTooLongException;
import org.example.requestmanager.domain.exception.InvalidTransitionException;
import org.example.requestmanager.domain.exception.ReasonRequiredException;
import org.example.requestmanager.domain.exception.RequestDomainException;
import org.example.requestmanager.domain.exception.RequiredFieldMissingException;
import org.example.requestmanager.domain.model.RequestAction;
import org.example.requestmanager.domain.model.RequestId;
import org.example.requestmanager.domain.model.RequestState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RequestController.class)
class ErrorHandlerTest {

    private static final UUID ID = UUID.randomUUID();

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

    // paging

    @Test
    void pageSizeOutOfRangeIsBadRequest() throws Exception {
        expectProblem(mockMvc.perform(get("/requests").param("size", "101")), 400)
                .andExpect(jsonPath("$.detail").value(containsString("size between 1 and 100")));
        verifyNoInteractions(browseRequests);
    }

    @Test
    void negativePageIsBadRequest() throws Exception {
        expectProblem(mockMvc.perform(get("/requests").param("page", "-1")), 400);
        verifyNoInteractions(browseRequests);
    }

    @Test
    void unknownStateFilterIsBadRequest() throws Exception {
        expectProblem(mockMvc.perform(get("/requests").param("state", "FOO")), 400);
        verifyNoInteractions(browseRequests);
    }

    // domain and application exceptions

    @Test
    void missingFieldIsBadRequest() throws Exception {
        when(createRequest.create(any(), any())).thenThrow(new RequiredFieldMissingException("name"));

        expectProblem(createRequest("{\"name\": \" \", \"content\": \"content\"}"), 400)
                .andExpect(jsonPath("$.detail").value("name must not be blank"))
                .andExpect(jsonPath("$.field").value("name"));
    }

    @Test
    void tooLongFieldIsBadRequest() throws Exception {
        when(createRequest.create(any(), any())).thenThrow(new FieldTooLongException("name", 255));

        expectProblem(createRequest("{\"name\": \"long\", \"content\": \"content\"}"), 400)
                .andExpect(jsonPath("$.detail").value("name must not be longer than 255 characters"))
                .andExpect(jsonPath("$.field").value("name"))
                .andExpect(jsonPath("$.maxLength").value(255));
    }

    @Test
    void missingReasonIsBadRequest() throws Exception {
        when(changeRequestState.changeState(any(), any(), any()))
                .thenThrow(new ReasonRequiredException(RequestAction.DELETE));

        expectProblem(transition("{\"action\": \"DELETE\"}"), 400)
                .andExpect(jsonPath("$.detail").value("Action DELETE requires a reason"))
                .andExpect(jsonPath("$.action").value("DELETE"));
    }

    @Test
    void invalidTransitionIsConflict() throws Exception {
        when(changeRequestState.changeState(any(), any(), any()))
                .thenThrow(new InvalidTransitionException(RequestState.CREATED, RequestAction.PUBLISH));

        expectProblem(transition("{\"action\": \"PUBLISH\"}"), 409)
                .andExpect(jsonPath("$.detail").value("Action PUBLISH is not allowed in state CREATED"))
                .andExpect(jsonPath("$.state").value("CREATED"))
                .andExpect(jsonPath("$.action").value("PUBLISH"));
    }

    @Test
    void contentNotEditableIsConflict() throws Exception {
        when(editRequestContent.editContent(any(), any()))
                .thenThrow(new ContentNotEditableException(RequestState.ACCEPTED));

        expectProblem(mockMvc.perform(put("/requests/{id}/content", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"new content\"}")), 409)
                .andExpect(jsonPath("$.detail").value("Content cannot be edited in state ACCEPTED"))
                .andExpect(jsonPath("$.state").value("ACCEPTED"));
    }

    @Test
    void unknownRequestIsNotFound() throws Exception {
        when(getRequest.get(any())).thenThrow(new RequestNotFoundException(new RequestId(ID)));

        expectProblem(mockMvc.perform(get("/requests/{id}", ID)), 404)
                .andExpect(jsonPath("$.detail").value("Request " + ID + " not found"))
                .andExpect(jsonPath("$.id").value(ID.toString()));
    }

    @Test
    void otherBusinessRuleViolationIsUnprocessable() throws Exception {
        when(getRequest.get(any())).thenThrow(new RequestDomainException("some other rule") {
        });

        expectProblem(mockMvc.perform(get("/requests/{id}", ID)), 422)
                .andExpect(jsonPath("$.detail").value("some other rule"));
    }

    // malformed HTTP input

    @Test
    void malformedJsonIsBadRequest() throws Exception {
        expectProblem(createRequest("{not json"), 400);
        verifyNoInteractions(createRequest);
    }

    @Test
    void unknownActionIsBadRequest() throws Exception {
        expectProblem(transition("{\"action\": \"FOO\"}"), 400);
        verifyNoInteractions(changeRequestState);
    }

    @Test
    void missingActionIsBadRequest() throws Exception {
        expectProblem(transition("{\"reason\": \"duplicate\"}"), 400);
        verifyNoInteractions(changeRequestState);
    }

    @Test
    void nonUuidIdIsBadRequest() throws Exception {
        expectProblem(mockMvc.perform(get("/requests/{id}", "not-a-uuid")), 400);
        verifyNoInteractions(getRequest);
    }

    private ResultActions createRequest(String json) throws Exception {
        return mockMvc.perform(post("/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private ResultActions transition(String json) throws Exception {
        return mockMvc.perform(post("/requests/{id}/transitions", ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private static ResultActions expectProblem(ResultActions result, int status) throws Exception {
        return result
                .andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(status));
    }
}
