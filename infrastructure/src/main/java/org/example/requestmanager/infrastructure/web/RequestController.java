package org.example.requestmanager.infrastructure.web;

import org.example.requestmanager.application.port.in.BrowseRequestsQuery;
import org.example.requestmanager.application.port.in.ChangeRequestStateUseCase;
import org.example.requestmanager.application.port.in.CreateRequestUseCase;
import org.example.requestmanager.application.port.in.EditRequestContentUseCase;
import org.example.requestmanager.application.port.in.GetRequestQuery;
import org.example.requestmanager.application.port.in.PageQuery;
import org.example.requestmanager.application.port.in.RequestFilter;
import org.example.requestmanager.domain.model.RequestId;
import org.example.requestmanager.domain.model.RequestRoot;
import org.example.requestmanager.domain.model.RequestState;
import org.example.requestmanager.infrastructure.web.dto.CreateRequestBody;
import org.example.requestmanager.infrastructure.web.dto.EditContentBody;
import org.example.requestmanager.infrastructure.web.dto.PageResponse;
import org.example.requestmanager.infrastructure.web.dto.RequestResponse;
import org.example.requestmanager.infrastructure.web.dto.RequestSummaryResponse;
import org.example.requestmanager.infrastructure.web.dto.StateChangeResponse;
import org.example.requestmanager.infrastructure.web.dto.TransitionBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/requests")
class RequestController {

    private final CreateRequestUseCase createRequest;
    private final EditRequestContentUseCase editRequestContent;
    private final ChangeRequestStateUseCase changeRequestState;
    private final GetRequestQuery getRequest;
    private final BrowseRequestsQuery browseRequests;

    RequestController(CreateRequestUseCase createRequest,
                      EditRequestContentUseCase editRequestContent,
                      ChangeRequestStateUseCase changeRequestState,
                      GetRequestQuery getRequest,
                      BrowseRequestsQuery browseRequests) {
        this.createRequest = createRequest;
        this.editRequestContent = editRequestContent;
        this.changeRequestState = changeRequestState;
        this.getRequest = getRequest;
        this.browseRequests = browseRequests;
    }

    @GetMapping
    PageResponse<RequestSummaryResponse> browse(@RequestParam(required = false) String name,
                                                @RequestParam(required = false) RequestState state,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "" + PageQuery.DEFAULT_SIZE) int size) {
        return PageResponse.from(
                browseRequests.browse(new RequestFilter(name, state), new PageQuery(page, size)),
                RequestSummaryResponse::from);
    }

    @PostMapping
    ResponseEntity<RequestResponse> create(@RequestBody CreateRequestBody body) {
        RequestRoot request = createRequest.create(body.name(), body.content());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(request.getId().id())
                .toUri();
        return ResponseEntity.created(location).body(RequestResponse.from(request));
    }

    @GetMapping("/{id}")
    RequestResponse get(@PathVariable UUID id) {
        return RequestResponse.from(getRequest.get(new RequestId(id)));
    }

    @PutMapping("/{id}/content")
    RequestResponse editContent(@PathVariable UUID id, @RequestBody EditContentBody body) {
        return RequestResponse.from(editRequestContent.editContent(new RequestId(id), body.content()));
    }

    @PostMapping("/{id}/transitions")
    RequestResponse changeState(@PathVariable UUID id, @RequestBody TransitionBody body) {
        return RequestResponse.from(changeRequestState.changeState(new RequestId(id), body.action(), body.reason()));
    }

    @GetMapping("/{id}/history")
    List<StateChangeResponse> history(@PathVariable UUID id) {
        return getRequest.get(new RequestId(id)).getHistory().stream()
                .map(StateChangeResponse::from)
                .toList();
    }
}
