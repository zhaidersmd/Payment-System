package com.paymentplatform.payment.concurrency;

import com.paymentplatform.payment.entity.Payment;
import com.paymentplatform.payment.entity.PaymentStatus;
import com.paymentplatform.payment.exception.InvalidPaymentStateException;
import com.paymentplatform.payment.repository.PaymentRepository;
import com.paymentplatform.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Testcontainers
public class PaymentConcurrencyTest {

    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:16").withDatabaseName("paymentdb").withUsername("payment_user").withPassword("payment_password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);

        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);

        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    }


    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        Payment payment = new Payment();
        payment.setCustomerId("concurrency-test");
        payment.setAmount(new BigDecimal("1000.00"));
        payment.setCurrency("INR");
        payment.setStatus(PaymentStatus.CREATED);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.saveAndFlush(payment);
    }

    @Test
    void concurrentUpdatesShouldBeHandledSafely() throws Exception {

        Payment payment = paymentRepository.findAll().getFirst();

        UUID paymentId = payment.getId();
        int threadCount = 10;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        CountDownLatch ready = new CountDownLatch(3);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {

            futures.add(executor.submit(() -> {

                ready.countDown();
                start.await();

                try {
                    paymentService.authorizePayment(paymentId);
                    return "SUCCESS";

                } catch (OptimisticLockingFailureException exception) {
                    return "OPTIMISTIC_LOCK";
                } catch (InvalidPaymentStateException exception) {
                    return "INVALID_STATE";
                }
            }));
        }
        System.out.println(futures.toString());

        ready.await();
        start.countDown();

        int success = 0;
        int optimisticLockFailures = 0;
        int invalidStateFailures = 0;

        for (Future<String> future : futures) {

            String result = future.get();

            switch (result) {

                case "SUCCESS" -> success++;
                case "OPTIMISTIC_LOCK" -> optimisticLockFailures++;
                case "INVALID_STATE" -> invalidStateFailures++;
            }
        }

        executor.shutdown();
        Payment updatedPayment = paymentRepository.findById(paymentId).orElseThrow();
        assertEquals(PaymentStatus.AUTHORIZED, updatedPayment.getStatus());

        assertEquals(1, success);

        System.out.println("SUCCESS = " + success + ", OPTIMISTIC_LOCK = " + optimisticLockFailures + ", INVALID_STATE = " + invalidStateFailures);
    }


}
