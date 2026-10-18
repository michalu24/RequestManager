package org.example.requestmanager.infrastructure.persistence;

import org.example.requestmanager.application.port.in.PageQuery;
import org.example.requestmanager.application.port.in.PageResult;
import org.example.requestmanager.application.port.in.RequestFilter;
import org.example.requestmanager.application.port.in.RequestSummary;
import org.example.requestmanager.domain.model.PublicationNumber;
import org.example.requestmanager.domain.model.RequestId;
import org.example.requestmanager.domain.model.RequestRoot;
import org.example.requestmanager.domain.model.RequestState;
import org.example.requestmanager.domain.transition.DiagramTransitionPolicy;
import org.example.requestmanager.domain.transition.TransitionPolicy;
import org.example.requestmanager.infrastructure.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({TestcontainersConfiguration.class, DiagramTransitionPolicy.class, RequestPersistenceAdapter.class})
class RequestPersistenceAdapterTest {

    // Postgres stores microseconds, so test timestamps avoid nanosecond precision
    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant LATER = NOW.plus(1, ChronoUnit.HOURS);
    private static final TransitionPolicy POLICY = new DiagramTransitionPolicy();

    @Autowired
    private RequestPersistenceAdapter adapter;

    @Autowired
    private TestEntityManager entityManager;

    // search

    @Test
    void searchesByNameFragmentIgnoringCase() {
        saveAll(request("Invoice March"), request("monthly INVOICE"), request("Order"));

        PageResult<RequestSummary> result = adapter.search(new RequestFilter("invoice", null), new PageQuery(0, 10));

        assertThat(result.items()).extracting(RequestSummary::name)
                .containsExactly("Invoice March", "monthly INVOICE");
        assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    void treatsLikeWildcardsInNameFilterLiterally() {
        saveAll(request("100% done"), request("1000 items"), request("a_b"), request("axb"));

        assertThat(adapter.search(new RequestFilter("0%", null), new PageQuery(0, 10)).items())
                .extracting(RequestSummary::name).containsExactly("100% done");
        assertThat(adapter.search(new RequestFilter("a_b", null), new PageQuery(0, 10)).items())
                .extracting(RequestSummary::name).containsExactly("a_b");
    }

    @Test
    void searchesByState() {
        RequestRoot verified = request("B");
        verified.verify(LATER);
        saveAll(request("A"), verified);

        PageResult<RequestSummary> result =
                adapter.search(new RequestFilter(null, RequestState.VERIFIED), new PageQuery(0, 10));

        assertThat(result.items()).singleElement()
                .satisfies(summary -> {
                    assertThat(summary.id()).isEqualTo(verified.getId());
                    assertThat(summary.state()).isEqualTo(RequestState.VERIFIED);
                });
    }

    @Test
    void searchesByNameAndStateTogether() {
        RequestRoot matching = request("Invoice 1");
        matching.verify(LATER);
        RequestRoot otherName = request("Order 1");
        otherName.verify(LATER);
        saveAll(matching, otherName, request("Invoice 2"));

        PageResult<RequestSummary> result =
                adapter.search(new RequestFilter("invoice", RequestState.VERIFIED), new PageQuery(0, 10));

        assertThat(result.items()).extracting(RequestSummary::id).containsExactly(matching.getId());
    }

    @Test
    void returnsRequestedPageOrderedByName() {
        saveAll(request("E"), request("C"), request("A"), request("D"), request("B"));

        PageResult<RequestSummary> secondPage = adapter.search(new RequestFilter(null, null), new PageQuery(1, 2));

        assertThat(secondPage.items()).extracting(RequestSummary::name).containsExactly("C", "D");
        assertThat(secondPage.totalElements()).isEqualTo(5);
        assertThat(secondPage.totalPages()).isEqualTo(3);
    }

    @Test
    void searchReturnsPublicationNumber() {
        saveAll(publishedRequest(9));

        PageResult<RequestSummary> result = adapter.search(new RequestFilter(null, null), new PageQuery(0, 10));

        assertThat(result.items()).singleElement()
                .satisfies(summary -> assertThat(summary.publicationNumber()).isEqualTo(new PublicationNumber(9)));
    }

    // save and load

    @Test
    void storesAndLoadsRequestWithFullHistory() {
        RequestRoot request = RequestRoot.create("name", "content", POLICY, NOW);
        request.verify(LATER);
        request.accept(LATER);
        request.publish(new PublicationNumber(7), LATER);

        adapter.save(request);
        flushAndClear();

        RequestRoot loaded = adapter.findById(request.getId()).orElseThrow();
        assertThat(loaded.getId()).isEqualTo(request.getId());
        assertThat(loaded.getName()).isEqualTo("name");
        assertThat(loaded.getContent()).isEqualTo("content");
        assertThat(loaded.getState()).isEqualTo(RequestState.PUBLISHED);
        assertThat(loaded.getPublicationNumber()).isEqualTo(new PublicationNumber(7));
        assertThat(loaded.getHistory()).containsExactlyElementsOf(request.getHistory());
    }

    @Test
    void appendsNewHistoryEntriesOnSubsequentSave() {
        RequestRoot request = RequestRoot.create("name", "content", POLICY, NOW);
        adapter.save(request);
        flushAndClear();

        RequestRoot loaded = adapter.findById(request.getId()).orElseThrow();
        loaded.editContent("new content");
        loaded.delete("duplicate", LATER);
        adapter.save(loaded);
        flushAndClear();

        RequestRoot reloaded = adapter.findById(request.getId()).orElseThrow();
        assertThat(reloaded.getContent()).isEqualTo("new content");
        assertThat(reloaded.getState()).isEqualTo(RequestState.DELETED);
        assertThat(reloaded.getHistory()).containsExactlyElementsOf(loaded.getHistory());
        assertThat(reloaded.getHistory()).hasSize(2);
    }

    @Test
    void loadedRequestFollowsTheTransitionPolicy() {
        RequestRoot request = RequestRoot.create("name", "content", POLICY, NOW);
        adapter.save(request);
        flushAndClear();

        RequestRoot loaded = adapter.findById(request.getId()).orElseThrow();
        loaded.verify(LATER);

        assertThat(loaded.getState()).isEqualTo(RequestState.VERIFIED);
    }

    @Test
    void databaseRejectsDuplicatePublicationNumber() {
        adapter.save(publishedRequest(42));
        flushAndClear();

        assertThatThrownBy(() -> {
            adapter.save(publishedRequest(42));
            entityManager.flush();
        }).rootCause().hasMessageContaining("request_publication_number_key");
    }

    @Test
    void returnsEmptyForUnknownId() {
        assertThat(adapter.findById(new RequestId(UUID.randomUUID()))).isEmpty();
    }

    private RequestRoot publishedRequest(long number) {
        RequestRoot request = RequestRoot.create("name", "content", POLICY, NOW);
        request.verify(NOW);
        request.accept(NOW);
        request.publish(new PublicationNumber(number), NOW);
        return request;
    }

    private static RequestRoot request(String name) {
        return RequestRoot.create(name, "content", POLICY, NOW);
    }

    private void saveAll(RequestRoot... requests) {
        for (RequestRoot request : requests) {
            adapter.save(request);
        }
        flushAndClear();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
