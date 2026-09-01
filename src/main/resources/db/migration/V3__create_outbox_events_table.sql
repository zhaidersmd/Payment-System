create table outbox_events
(
    id             UUID primary key,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id   UUID         NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    payload        TEXT         NOT NULL,
    status         VARCHAR(30)  NOT NULL,
    retry_count    INTEGER      NOT NULL DEFAULT 0,
    created_at     TIMESTAMP    NOT NULL,
    published_at   TIMESTAMP NULL
)