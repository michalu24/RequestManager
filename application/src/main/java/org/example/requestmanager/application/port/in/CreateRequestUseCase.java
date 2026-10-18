package org.example.requestmanager.application.port.in;

import org.example.requestmanager.domain.model.RequestRoot;

public interface CreateRequestUseCase {

    RequestRoot create(String name, String content);
}
