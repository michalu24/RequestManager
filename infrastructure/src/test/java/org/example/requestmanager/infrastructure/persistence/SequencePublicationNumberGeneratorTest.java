package org.example.requestmanager.infrastructure.persistence;

import org.example.requestmanager.domain.model.PublicationNumber;
import org.example.requestmanager.infrastructure.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({TestcontainersConfiguration.class, SequencePublicationNumberGenerator.class})
class SequencePublicationNumberGeneratorTest {

    @Autowired
    private SequencePublicationNumberGenerator generator;

    @Test
    void returnsPositiveNumbers() {
        assertThat(generator.next().value()).isPositive();
    }

    @Test
    void returnsIncreasingNumbers() {
        long first = generator.next().value();
        long second = generator.next().value();
        long third = generator.next().value();

        assertThat(second).isGreaterThan(first);
        assertThat(third).isGreaterThan(second);
    }

    @Test
    void neverReturnsTheSameNumberToConcurrentCallers() throws Exception {
        int threads = 8;
        int callsPerThread = 25;
        Callable<List<PublicationNumber>> task = () -> IntStream.range(0, callsPerThread)
                .mapToObj(i -> generator.next())
                .toList();

        List<PublicationNumber> numbers;
        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            List<Future<List<PublicationNumber>>> futures = executor.invokeAll(
                    IntStream.range(0, threads).mapToObj(i -> task).toList());
            numbers = futures.stream()
                    .flatMap(future -> {
                        try {
                            return future.get().stream();
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .toList();
        }

        assertThat(numbers)
                .hasSize(threads * callsPerThread)
                .doesNotHaveDuplicates();
    }
}
