package com.paymentplatform.payment.util;

import com.paymentplatform.payment.dto.CreatePaymentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class RequestHashUtil {

    private static final Logger log = LoggerFactory.getLogger(RequestHashUtil.class);

    private RequestHashUtil() {

    }





        public static String generateHash(CreatePaymentRequest request) {

            String canonicalRequest =
                    request.customerId()+ "|" + request.amount().stripTrailingZeros().toPlainString()
                            + "|" + request.currency().toUpperCase();
            //log.info("Canonical request: {}", canonicalRequest);

            try {
                MessageDigest digest =
                        MessageDigest.getInstance("SHA-256");

                byte[] hash =
                        digest.digest(
                                canonicalRequest.getBytes(StandardCharsets.UTF_8)
                        );

                StringBuilder hexString = new StringBuilder();

                for (byte b : hash) {
                    hexString.append(
                            String.format("%02x", b)
                    );
                }

                return hexString.toString();

            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(
                        "SHA-256 algorithm not available", e);
            }
        }
    }

