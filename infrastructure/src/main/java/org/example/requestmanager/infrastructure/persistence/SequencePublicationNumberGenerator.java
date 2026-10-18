package org.example.requestmanager.infrastructure.persistence;

import org.example.requestmanager.application.port.out.PublicationNumberGenerator;
import org.example.requestmanager.domain.model.PublicationNumber;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Takes publication numbers from a database sequence. nextval is atomic, so concurrent
 * callers never receive the same number. Values are not returned to the sequence on
 * rollback, which may leave gaps; uniqueness, not continuity, is required.
 */
@Component
public class SequencePublicationNumberGenerator implements PublicationNumberGenerator {

    private final JdbcClient jdbcClient;

    SequencePublicationNumberGenerator(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public PublicationNumber next() {
        Long value = jdbcClient.sql("SELECT nextval('publication_number_seq')")
                .query(Long.class)
                .single();
        return new PublicationNumber(value);
    }
}
