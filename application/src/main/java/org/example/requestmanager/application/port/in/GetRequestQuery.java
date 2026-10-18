package org.example.requestmanager.application.port.in;

import org.example.requestmanager.domain.model.RequestId;
import org.example.requestmanager.domain.model.RequestRoot;

public interface GetRequestQuery {

    RequestRoot get(RequestId id);
}
