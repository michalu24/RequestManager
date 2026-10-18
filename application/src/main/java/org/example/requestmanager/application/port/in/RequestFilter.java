package org.example.requestmanager.application.port.in;

import org.example.requestmanager.domain.model.RequestState;

/**
 * Criteria for browsing requests; a {@code null} field means "no filter".
 * {@code name} matches any part of the request name, ignoring case.
 */
public record RequestFilter(String name, RequestState state) {
}
