package org.example.requestmanager.infrastructure.persistence;

import org.example.requestmanager.domain.model.RequestState;

import java.util.UUID;

/**
 * Spring Data projection for listing requests.
 */
public interface RequestSummaryRow {

    UUID getId();

    String getName();

    RequestState getState();

    Long getPublicationNumber();
}
