package com.riakgu.digilo.payment;

import com.riakgu.digilo.common.exception.PaymentGatewayException;
import com.riakgu.digilo.config.MidtransProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

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
    private final RestClient.Builder restClientBuilder;

    public Map<String, Object> createQrisCharge(String orderId, long amount) {
        String url = midtransProperties.baseUrl() + "/v2/charge";

        Map<String, Object> transactionDetails = new HashMap<>();
        transactionDetails.put("order_id", orderId);
        transactionDetails.put("gross_amount", amount);

        Map<String, Object> qris = new HashMap<>();
        qris.put("acquirer", "gopay");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("payment_type", "qris");
        requestBody.put("transaction_details", transactionDetails);
        requestBody.put("qris", qris);

        try {
            RestClient client = restClientBuilder.build();
            Map<String, Object> response = client.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", createAuthHeader())
                    .body(requestBody)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            log.info("Midtrans QRIS charge response: {}", response);
            return response;
        } catch (Exception e) {
            log.error("Failed to create QRIS charge", e);
            throw new PaymentGatewayException("Failed to create payment: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> getTransactionStatus(String orderId) {
        String url = midtransProperties.baseUrl() + "/v2/" + orderId + "/status";

        try {
            RestClient client = restClientBuilder.build();
            Map<String, Object> response = client.get()
                    .uri(url)
                    .header("Authorization", createAuthHeader())
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            log.info("Midtrans status response: {}", response);
            return response;
        } catch (Exception e) {
            log.error("Failed to get transaction status", e);
            throw new PaymentGatewayException("Failed to get payment status: " + e.getMessage(), e);
        }
    }

    public boolean cancelTransaction(String orderId) {
        String url = midtransProperties.baseUrl() + "/v2/" + orderId + "/cancel";

        try {
            RestClient client = restClientBuilder.build();
            Map<String, Object> response = client.post()
                    .uri(url)
                    .header("Authorization", createAuthHeader())
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            log.info("Midtrans cancel response: {}", response);
            
            String statusCode = (String) response.get("status_code");
            return "200".equals(statusCode);
        } catch (Exception e) {
            log.error("Failed to cancel transaction {}: {}", orderId, e.getMessage());
            return false;
        }
    }

    public boolean verifySignature(String orderId, String statusCode, String grossAmount, String signatureKey) {
        try {
            String rawString = orderId + statusCode + grossAmount + midtransProperties.serverKey();
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
            case "deny" -> PaymentStatus.FAILED;
            case "cancel" -> PaymentStatus.CANCELLED;
            case "expire" -> PaymentStatus.EXPIRED;
            case "refund", "partial_refund" -> PaymentStatus.REFUNDED;
            default -> PaymentStatus.PENDING;
        };
    }

    public Instant calculateExpiry() {
        return Instant.now().plus(15, ChronoUnit.MINUTES);
    }

    private String createAuthHeader() {
        String auth = midtransProperties.serverKey() + ":";
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encodedAuth;
    }
}