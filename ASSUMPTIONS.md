# Assumptions

The task description leaves some points to the implementer's judgement. This document lists
the decisions made and the reasoning behind them.

## Technology

- **Java 21 with Spring Boot 4.1.** The task names no language or framework.
- **PostgreSQL** as the database (the task allows SQL or NoSQL). A sequence with a `UNIQUE`
  constraint gives unique publication numbers that are safe under concurrent publishing.
- **Flyway** manages the schema. The migrations are source code and are part of the
  repository; the database itself (its data and files) is not.
- **Docker is required** to run the application (PostgreSQL from `docker-compose.yml`,
  started automatically by Spring Boot) and to run the tests (Testcontainers).
- **No authentication or authorization.** The task does not mention users or roles.

## Request data

- A value is **missing** when it is `null`, empty or contains only whitespace. This applies
  to `name`, `content` and `reason`.
- `name` is **at most 255 characters**. `content` and `reason` have no length limit.
- `name` **cannot be changed** after creation; the task allows editing only the body (content).
- Values are stored as sent; they are not trimmed.
- Timestamps in the history are recorded in **UTC**.

## Lifecycle

- Transitions follow the diagram exactly. `DELETED`, `REJECTED` and `PUBLISHED` are
  **final states**; no action is allowed from them.
- A request comes into existence only through `POST /requests`. Sending `CREATE` as a
  transition is treated as a transition not allowed by the diagram (`409`).
- **Delete is a state, not a removal.** A deleted request and its history stay in the
  database, so the audit log remains complete.
- The **reason** for delete and reject is stored in the history entry of that transition.
- Content can be edited only in `CREATED` and `VERIFIED`. Editing **is not recorded in the
  history**, because the task asks for the history of state changes and editing does not
  change the state.

## Publication number

- A positive number taken from a database sequence when the request is published.
- The task requires the number to be **unique, not continuous**. A number taken by a publish
  that is later rolled back is not reused, so gaps in the numbering are possible.

## Audit log

- Every state change is stored with: previous state, new state, action, reason (for delete
  and reject) and time. Creation is the first entry (no previous state).
- The history is available at `GET /requests/{id}/history`, ordered from oldest to newest.

## Browsing (optional part of the task)

- `name` filter matches **any part of the name, ignoring case**. Characters `%` and `_` are
  matched literally.
- `state` filter matches exactly. Both filters are optional and can be combined.
- Pages are numbered **from 0**. Default page size is **10** (as required), maximum is **100**.
- Results are ordered by name, then by id, so paging is stable.
- The list contains id, name, state and publication number; content and history are
  available from the single request endpoints.

## API

- Errors use RFC 9457 problem details. Status codes:
  `400` invalid input, `404` unknown request, `409` action not allowed in the current state,
  `422` any other business rule violation.
- State changes use one endpoint, `POST /requests/{id}/transitions`, with the action in the
  body. It mirrors the diagram, which is defined as data, rather than one endpoint per action.

## Known limitations

- **No optimistic locking.** Each use case runs in one transaction, but two concurrent
  requests changing the same request may overwrite each other's result. A `version` column
  would prevent this; it was left out as the task does not cover concurrent editing.
- **No authentication or authorization** (see above).
