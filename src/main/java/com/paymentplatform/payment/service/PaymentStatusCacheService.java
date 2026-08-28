package com.paymentplatform.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.payment.dto.PaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.UUID;

@Service
public class PaymentStatusCacheService {

    private static final Duration CACHE_TTL =
            Duration.ofSeconds(60);
    private static final Logger log = LoggerFactory.getLogger(PaymentStatusCacheService.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public PaymentStatusCacheService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }



    public void put(
            UUID paymentId,
            PaymentResponse response) {

        String key = buildKey(paymentId);

        try {
            String value =
                    objectMapper.writeValueAsString(response);

            redisTemplate.opsForValue().set(
                    key,
                    value,
                    CACHE_TTL);

        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to serialize payment status",
                    e);
        }
        catch (RuntimeException ignored) {



        }
    }

    public void evict(UUID paymentId) {

        try {
            redisTemplate.delete(
                    buildKey(paymentId));
        } catch (RuntimeException ignored) {



        }
    }

    private String buildKey(UUID paymentId) {
        return "payment:status:" + paymentId;
    }

    public PaymentResponse get(UUID paymentId) {
        String key = buildKey(paymentId);
        try {
            String cachedValue = redisTemplate.opsForValue().get(key);

            if (cachedValue == null) {
                return null;
            }
            return objectMapper.readValue(
                    cachedValue,
                    PaymentResponse.class);
        } catch (RuntimeException ignored) {
            return null;
        } catch (JsonProcessingException e) {
            redisTemplate.delete(key);
            return null;
        }
    }
}