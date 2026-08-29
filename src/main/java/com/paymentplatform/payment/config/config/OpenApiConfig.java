package com.paymentplatform.payment.config.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Distributed Payment Processing Platform API")
                        .description("""
                                Payment processing APIs supporting payment lifecycle,
                                idempotency, transactional outbox publishing,
                                Redis caching, and Kafka event processing.
                                """)
                        .version("v1.0.0"));
    }
    }

