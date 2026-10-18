package org.example.requestmanager.application.port.in;

import org.example.requestmanager.domain.model.PublicationNumber;
import org.example.requestmanager.domain.model.RequestId;
import org.example.requestmanager.domain.model.RequestState;

/**
 * Read model for listings: no content and no history, so browsing does not load them.
 */
public record RequestSummary(RequestId id, String name, RequestState state, PublicationNumber publicationNumber) {
}
