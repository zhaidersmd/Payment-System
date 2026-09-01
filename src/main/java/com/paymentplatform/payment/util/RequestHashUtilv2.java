package com.paymentplatform.payment.util;

import com.paymentplatform.payment.dto.CreatePaymentRequest;
import com.paymentplatform.payment.dto.v2.CreatePaymentRequestV2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class RequestHashUtilv2 {

    private static final Logger log = LoggerFactory.getLogger(RequestHashUtilv2.class);

    private RequestHashUtilv2() {

    }





        public static String generateHash(CreatePaymentRequestV2 request) {

            String canonicalRequest =
                    request.getCustomerId()+ "|" + request.getAmount().stripTrailingZeros().toPlainString()
                            + "|" + request.getCurrency().toUpperCase() + "|" + request.getPaymentMethod();


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

