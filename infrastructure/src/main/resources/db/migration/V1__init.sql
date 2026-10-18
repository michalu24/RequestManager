CREATE TABLE request (
    id                 UUID         PRIMARY KEY,
    name               VARCHAR(255) NOT NULL,
    content            TEXT         NOT NULL,
    state              VARCHAR(20)  NOT NULL,
    publication_number BIGINT       UNIQUE
);

CREATE TABLE state_change (
    id          BIGSERIAL   PRIMARY KEY,
    request_id  UUID        NOT NULL REFERENCES request (id),
    from_state  VARCHAR(20),
    to_state    VARCHAR(20) NOT NULL,
    action      VARCHAR(20) NOT NULL,
    reason      TEXT,
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_state_change_request_id ON state_change (request_id);

CREATE SEQUENCE publication_number_seq;
