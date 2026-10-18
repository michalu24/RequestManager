package org.example.requestmanager.application.port.in;

import org.example.requestmanager.domain.model.RequestAction;
import org.example.requestmanager.domain.model.RequestId;
import org.example.requestmanager.domain.model.RequestRoot;

public interface ChangeRequestStateUseCase {

    /**
     * Applies {@code action} to the request. {@code reason} is required for DELETE and REJECT
     * and ignored otherwise.
     */
    RequestRoot changeState(RequestId id, RequestAction action, String reason);
}
