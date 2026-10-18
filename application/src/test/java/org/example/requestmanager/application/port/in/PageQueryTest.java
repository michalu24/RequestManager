package org.example.requestmanager.application.port.in;

import org.example.requestmanager.application.exception.InvalidPageRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageQueryTest {

    @Test
    void defaultPageSizeIsTen() {
        assertThat(PageQuery.DEFAULT_SIZE).isEqualTo(10);
    }

    @ParameterizedTest
    @CsvSource({"0, 1", "0, 100", "5, 10"})
    void acceptsValidPage(int page, int size) {
        assertThatNoException().isThrownBy(() -> new PageQuery(page, size));
    }

    @ParameterizedTest
    @CsvSource({"-1, 10", "0, 0", "0, 101"})
    void rejectsInvalidPage(int page, int size) {
        assertThatThrownBy(() -> new PageQuery(page, size))
                .isInstanceOf(InvalidPageRequestException.class);
    }

    @Test
    void computesTotalPages() {
        assertThat(new PageResult<>(List.of(), 0, 10, 0).totalPages()).isZero();
        assertThat(new PageResult<>(List.of(), 0, 10, 10).totalPages()).isEqualTo(1);
        assertThat(new PageResult<>(List.of(), 0, 10, 11).totalPages()).isEqualTo(2);
    }
}
