package org.example.requestmanager.application.service;

import org.example.requestmanager.application.port.out.PublicationNumberGenerator;
import org.example.requestmanager.domain.model.PublicationNumber;

class SequentialPublicationNumberGenerator implements PublicationNumberGenerator {

    private long last = 0;

    @Override
    public PublicationNumber next() {
        return new PublicationNumber(++last);
    }
}
