package org.example.requestmanager.domain.model;

public record PublicationNumber(long value) {

    public PublicationNumber {
        if (value <= 0) {
            throw new IllegalArgumentException("publication number must be positive");
        }
    }
}
