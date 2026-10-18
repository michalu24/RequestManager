package org.example.requestmanager.application.port.in;

import java.util.List;
import java.util.function.Function;

public record PageResult<T>(List<T> items, int page, int size, long totalElements) {

    public PageResult {
        items = List.copyOf(items);
    }

    public int totalPages() {
        return (int) Math.ceil((double) totalElements / size);
    }

    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(items.stream().map(mapper).toList(), page, size, totalElements);
    }
}
