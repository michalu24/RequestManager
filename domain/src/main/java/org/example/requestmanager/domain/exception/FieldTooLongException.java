package org.example.requestmanager.domain.exception;

public class FieldTooLongException extends RequestDomainException {

    private final String field;
    private final int maxLength;

    public FieldTooLongException(String field, int maxLength) {
        super(field + " must not be longer than " + maxLength + " characters");
        this.field = field;
        this.maxLength = maxLength;
    }

    public String getField() {
        return field;
    }

    public int getMaxLength() {
        return maxLength;
    }
}
