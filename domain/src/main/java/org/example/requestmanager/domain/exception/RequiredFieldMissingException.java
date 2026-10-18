package org.example.requestmanager.domain.exception;

public class RequiredFieldMissingException extends RequestDomainException {

    private final String field;

    public RequiredFieldMissingException(String field) {
        super(field + " must not be blank");
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
