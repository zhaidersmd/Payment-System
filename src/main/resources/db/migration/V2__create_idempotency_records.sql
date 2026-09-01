CREATE TABLE idempotency_records
(
    id              UUID PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL,
    request_hash    VARCHAR(64)  NOT NULL,
    payment_id      UUID         NOT NULL,
    response_status INTEGER      NOT NULL,
    response_body   TEXT         NOT NULL,
    created_at      TIMESTAMP    NOT NULL,

    CONSTRAINT uk_idempotency_key UNIQUE (idempotency_key),

    CONSTRAINT fk_idempotency_payment
        FOREIGN KEY (payment_id)
            REFERENCES payments (id)
);