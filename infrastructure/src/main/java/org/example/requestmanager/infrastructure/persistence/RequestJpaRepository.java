package org.example.requestmanager.infrastructure.persistence;

import org.example.requestmanager.domain.model.RequestState;
import org.example.requestmanager.infrastructure.persistence.entity.RequestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

interface RequestJpaRepository extends JpaRepository<RequestEntity, UUID> {

    /**
     * Selects only the listed columns, so history and content are not loaded.
     * {@code namePattern} is a lower-case LIKE pattern escaped with '\'; a null state disables its filter.
     */
    @Query("""
            SELECT r.id AS id, r.name AS name, r.state AS state, r.publicationNumber AS publicationNumber
            FROM RequestEntity r
            WHERE LOWER(r.name) LIKE :namePattern ESCAPE '\\'
              AND (:state IS NULL OR r.state = :state)
            """)
    Page<RequestSummaryRow> search(@Param("namePattern") String namePattern,
                                   @Param("state") RequestState state,
                                   Pageable pageable);
}
