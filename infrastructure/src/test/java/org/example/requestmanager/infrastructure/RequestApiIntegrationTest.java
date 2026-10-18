package org.example.requestmanager.infrastructure;

import org.example.requestmanager.infrastructure.web.dto.RequestResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests: real HTTP requests against the running application backed by
 * PostgreSQL in Testcontainers. Each test works on its own requests, so no cleanup is needed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
class RequestApiIntegrationTest {

    @Autowired
    private RestTestClient client;

    // happy paths

    @Test
    void walksThroughTheWholeLifecycle() {
        UUID id = create("name", "content");
        editContent(id, "edited");
        transition(id, Map.of("action", "VERIFY"));
        transition(id, Map.of("action", "ACCEPT"));
        transition(id, Map.of("action", "PUBLISH"));

        client.get().uri("/requests/{id}", id).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").isEqualTo("edited")
                .jsonPath("$.state").isEqualTo("PUBLISHED")
                .jsonPath("$.publicationNumber").isNumber();

        client.get().uri("/requests/{id}/history", id).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(4)
                .jsonPath("$[0].action").isEqualTo("CREATE")
                .jsonPath("$[1].action").isEqualTo("VERIFY")
                .jsonPath("$[2].action").isEqualTo("ACCEPT")
                .jsonPath("$[3].action").isEqualTo("PUBLISH")
                .jsonPath("$[3].from").isEqualTo("ACCEPTED")
                .jsonPath("$[3].to").isEqualTo("PUBLISHED");
    }

    @Test
    void assignsDifferentPublicationNumbers() {
        UUID first = publishedRequest();
        UUID second = publishedRequest();

        assertThat(get(first).publicationNumber())
                .isNotNull()
                .isNotEqualTo(get(second).publicationNumber());
    }

    @Test
    void recordsReasonOfDeletion() {
        UUID id = create("name", "content");
        transition(id, Map.of("action", "DELETE", "reason", "duplicate"));

        assertThat(get(id).state().name()).isEqualTo("DELETED");
        client.get().uri("/requests/{id}/history", id).exchange()
                .expectBody()
                .jsonPath("$[1].action").isEqualTo("DELETE")
                .jsonPath("$[1].reason").isEqualTo("duplicate");
    }

    @Test
    void recordsReasonOfRejection() {
        UUID id = create("name", "content");
        transition(id, Map.of("action", "VERIFY"));
        transition(id, Map.of("action", "ACCEPT"));
        transition(id, Map.of("action", "REJECT", "reason", "incomplete"));

        assertThat(get(id).state().name()).isEqualTo("REJECTED");
        client.get().uri("/requests/{id}/history", id).exchange()
                .expectBody()
                .jsonPath("$[3].from").isEqualTo("ACCEPTED")
                .jsonPath("$[3].reason").isEqualTo("incomplete");
    }

    // errors from the real domain

    @Test
    void refusesBlankName() {
        client.post().uri("/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("name", " ", "content", "content"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.field").isEqualTo("name");
    }

    @Test
    void refusesDeletionWithoutReasonAndKeepsRequest() {
        UUID id = create("name", "content");

        postTransition(id, Map.of("action", "DELETE"))
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.action").isEqualTo("DELETE");

        assertUnchanged(id, "CREATED", "content", 1);
    }

    @Test
    void refusesTransitionNotOnTheDiagramAndKeepsRequest() {
        UUID id = create("name", "content");

        postTransition(id, Map.of("action", "PUBLISH"))
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.state").isEqualTo("CREATED")
                .jsonPath("$.action").isEqualTo("PUBLISH");

        assertUnchanged(id, "CREATED", "content", 1);
        assertThat(get(id).publicationNumber()).isNull();
    }

    @Test
    void refusesContentEditOutsideEditableStatesAndKeepsContent() {
        UUID id = create("name", "content");
        transition(id, Map.of("action", "VERIFY"));
        transition(id, Map.of("action", "ACCEPT"));

        client.put().uri("/requests/{id}/content", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("content", "edited"))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.state").isEqualTo("ACCEPTED");

        assertUnchanged(id, "ACCEPTED", "content", 3);
    }

    @Test
    void refusesCreateAsTransition() {
        UUID id = create("name", "content");

        postTransition(id, Map.of("action", "CREATE"))
                .expectStatus().isEqualTo(409);

        assertUnchanged(id, "CREATED", "content", 1);
    }

    @Test
    void reportsUnknownRequest() {
        UUID unknown = UUID.randomUUID();

        client.get().uri("/requests/{id}", unknown).exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.id").isEqualTo(unknown.toString());
    }

    // helpers

    private UUID create(String name, String content) {
        RequestResponse created = client.post().uri("/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("name", name, "content", content))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("Location")
                .expectBody(RequestResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(created).isNotNull();
        return created.id();
    }

    private UUID publishedRequest() {
        UUID id = create("name", "content");
        transition(id, Map.of("action", "VERIFY"));
        transition(id, Map.of("action", "ACCEPT"));
        transition(id, Map.of("action", "PUBLISH"));
        return id;
    }

    private void editContent(UUID id, String content) {
        client.put().uri("/requests/{id}/content", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("content", content))
                .exchange()
                .expectStatus().isOk();
    }

    private void transition(UUID id, Map<String, String> body) {
        postTransition(id, body).expectStatus().isOk();
    }

    private RestTestClient.ResponseSpec postTransition(UUID id, Map<String, String> body) {
        return client.post().uri("/requests/{id}/transitions", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange();
    }

    private RequestResponse get(UUID id) {
        return client.get().uri("/requests/{id}", id).exchange()
                .expectStatus().isOk()
                .expectBody(RequestResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private void assertUnchanged(UUID id, String state, String content, int historySize) {
        RequestResponse request = get(id);
        assertThat(request.state().name()).isEqualTo(state);
        assertThat(request.content()).isEqualTo(content);
        client.get().uri("/requests/{id}/history", id).exchange()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(historySize);
    }
}
