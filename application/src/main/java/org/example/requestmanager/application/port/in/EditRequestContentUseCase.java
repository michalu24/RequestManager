package org.example.requestmanager.application.port.in;

import org.example.requestmanager.domain.model.RequestId;
import org.example.requestmanager.domain.model.RequestRoot;

public interface EditRequestContentUseCase {

    RequestRoot editContent(RequestId id, String content);
}
