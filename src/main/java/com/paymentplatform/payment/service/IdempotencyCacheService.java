package com.paymentplatform.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.payment.entity.IdempotencyRecord;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;


@Service
public class IdempotencyCacheService {

    private static final Duration CACHE_TTL =
            Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public IdempotencyCacheService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {

        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    private String buildKey(String idempotencyKey) {
        return "payment:idempotency:" + idempotencyKey;
    }

    public IdempotencyRecord get(String idempotencyKey) {
        String key = buildKey(idempotencyKey);

        try {
            String cachedValue = redisTemplate.opsForValue().get(key);
            if (cachedValue == null) {
                return null;
            }

            return objectMapper.readValue(cachedValue, IdempotencyRecord.class);
        } catch (RuntimeException e) {
            return null;

        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize idempotency record", e);
        }
    }

    public void put(
            String idempotencyKey,
            IdempotencyRecord record) {

        String key = buildKey(idempotencyKey);

        try {
            String value =
                    objectMapper.writeValueAsString(record);

            redisTemplate.opsForValue().set(
                    key,
                    value,
                    CACHE_TTL);

        } catch (RuntimeException e) {

            // Redis is only a cache.
            // Do not fail the payment operation.

        } catch (JsonProcessingException e) {

            throw new IllegalStateException(
                    "Failed to serialize idempotency record",
                    e);
        }
    }

    public void evict(String idempotencyKey) {

        try {
            redisTemplate.delete(
                    buildKey(idempotencyKey));

        } catch (RuntimeException e) {

            // Best effort cache eviction.
        }
    }


}
