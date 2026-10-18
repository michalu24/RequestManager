package org.example.requestmanager.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PublicationNumberTest {

    @Test
    void acceptsPositiveValue() {
        assertThat(new PublicationNumber(1).value()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void rejectsNonPositiveValue(long value) {
        assertThatThrownBy(() -> new PublicationNumber(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("publication number must be positive");
    }
}
