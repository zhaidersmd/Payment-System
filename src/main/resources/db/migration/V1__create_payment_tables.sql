CREATE TABLE payments (
                          id UUID PRIMARY KEY,
                          customer_id VARCHAR(50) NOT NULL,
                          amount NUMERIC(19, 2) NOT NULL,
                          currency VARCHAR(3) NOT NULL,
                          status VARCHAR(30) NOT NULL,
                          created_at TIMESTAMP NOT NULL,
                          updated_at TIMESTAMP NOT NULL,
                          version BIGINT NOT NULL
);