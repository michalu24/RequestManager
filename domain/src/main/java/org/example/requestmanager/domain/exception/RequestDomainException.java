package org.example.requestmanager.domain.exception;

/**
 * Base class for violations of request business rules.
 */
public abstract class RequestDomainException extends RuntimeException {

    protected RequestDomainException(String message) {
        super(message);
    }
}
