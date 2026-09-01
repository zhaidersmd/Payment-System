CREATE TABLE customers (

                           id UUID PRIMARY KEY,

                           customer_id VARCHAR(50) NOT NULL,

                           user_id UUID NOT NULL,

                           created_at TIMESTAMP NOT NULL,

                           CONSTRAINT uk_customers_customer_id
                               UNIQUE (customer_id),

                           CONSTRAINT uk_customers_user_id
                               UNIQUE (user_id),

                           CONSTRAINT fk_customers_user
                               FOREIGN KEY (user_id)
                                   REFERENCES users(id)
);