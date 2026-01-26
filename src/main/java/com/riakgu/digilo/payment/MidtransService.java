package com.riakgu.digilo.payment;

import com.riakgu.digilo.config.MidtransProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MidtransService {

    private final MidtransProperties midtransProperties;
    private final RestTemplate restTemplate;

    public Map<String, Object> createQrisCharge(String orderId, long amount) {
        String url = midtransProperties.getBaseUrl() + "/v2/charge";

        Map<String, Object> transactionDetails = new HashMap<>();
        transactionDetails.put("order_id", orderId);
        transactionDetails.put("gross_amount", amount);

        Map<String, Object> qris = new HashMap<>();
        qris.put("acquirer", "gopay");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("payment_type", "qris");
        requestBody.put("transaction_details", transactionDetails);
        requestBody.put("qris", qris);

        HttpHeaders headers = createHeaders();
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Map.class);

            log.info("Midtrans QRIS charge response: {}", response.getBody());
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to create QRIS charge", e);
            throw new RuntimeException("Failed to create payment: " + e.getMessage());
        }
    }

    public Map<String, Object> getTransactionStatus(String orderId) {
        String url = midtransProperties.getBaseUrl() + "/v2/" + orderId + "/status";

        HttpHeaders headers = createHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Map.class);

            log.info("Midtrans status response: {}", response.getBody());
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to get transaction status", e);
            throw new RuntimeException("Failed to get payment status: " + e.getMessage());
        }
    }

    public boolean verifySignature(String orderId, String statusCode, String grossAmount, String signatureKey) {
        try {
            String rawString = orderId + statusCode + grossAmount + midtransProperties.getServerKey();
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] digest = md.digest(rawString.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }

            String calculatedSignature = sb.toString();
            boolean isValid = calculatedSignature.equals(signatureKey);

            if (!isValid) {
                log.warn("Invalid signature for order {}", orderId);
            }

            return isValid;
        } catch (Exception e) {
            log.error("Failed to verify signature", e);
            return false;
        }
    }

    public PaymentStatus mapTransactionStatus(String transactionStatus, String fraudStatus) {
        if (transactionStatus == null) {
            return PaymentStatus.PENDING;
        }

        return switch (transactionStatus) {
            case "capture", "settlement" -> {
                // Check fraud status for capture
                if ("challenge".equals(fraudStatus)) {
                    yield PaymentStatus.PENDING;
                }
                yield PaymentStatus.SUCCESS;
            }
            case "pending" -> PaymentStatus.PENDING;
            case "deny", "cancel" -> PaymentStatus.FAILED;
            case "expire" -> PaymentStatus.EXPIRED;
            case "refund", "partial_refund" -> PaymentStatus.REFUNDED;
            default -> PaymentStatus.PENDING;
        };
    }

    public Instant calculateExpiry() {
        return Instant.now().plus(15, ChronoUnit.MINUTES);
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String auth = midtransProperties.getServerKey() + ":";
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedAuth);

        return headers;
    }
}