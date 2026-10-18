package org.example.requestmanager.application.port.out;

import org.example.requestmanager.domain.model.PublicationNumber;

public interface PublicationNumberGenerator {

    /**
     * Returns a publication number that has never been returned before.
     * Implementations are responsible for uniqueness across all requests.
     */
    PublicationNumber next();
}
