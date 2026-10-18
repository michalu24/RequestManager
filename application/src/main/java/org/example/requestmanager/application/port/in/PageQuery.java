package org.example.requestmanager.application.port.in;

import org.example.requestmanager.application.exception.InvalidPageRequestException;

/**
 * Zero-based page number and page size.
 */
public record PageQuery(int page, int size) {

    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 100;

    public PageQuery {
        if (page < 0 || size < 1 || size > MAX_SIZE) {
            throw new InvalidPageRequestException(page, size, MAX_SIZE);
        }
    }
}
