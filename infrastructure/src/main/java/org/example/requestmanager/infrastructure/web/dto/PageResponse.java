package org.example.requestmanager.infrastructure.web.dto;

import org.example.requestmanager.application.port.in.PageResult;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <S, T> PageResponse<T> from(PageResult<S> result, Function<S, T> mapper) {
        PageResult<T> mapped = result.map(mapper);
        return new PageResponse<>(
                mapped.items(),
                mapped.page(),
                mapped.size(),
                mapped.totalElements(),
                mapped.totalPages());
    }
}
