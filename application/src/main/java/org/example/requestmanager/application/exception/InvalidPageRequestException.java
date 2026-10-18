package org.example.requestmanager.application.exception;

public class InvalidPageRequestException extends RuntimeException {

    public InvalidPageRequestException(int page, int size, int maxSize) {
        super("Invalid page request: page must be >= 0 and size between 1 and " + maxSize
                + " (got page=" + page + ", size=" + size + ")");
    }
}
