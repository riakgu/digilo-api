package com.riakgu.digilo.config;

import com.riakgu.digilo.auth.GoogleAuthService;
import com.riakgu.digilo.common.service.StorageService;
import com.riakgu.digilo.common.service.WhatsAppService;
import com.riakgu.digilo.payment.MidtransService;
import com.riakgu.digilo.payment.PaymentStatus;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * Test configuration that provides mock beans for external services.
 * Import this class in tests that need mocked external dependencies.
 */
@TestConfiguration
public class TestMockConfig {

    @Bean
    @Primary
    public MidtransService midtransService() {
        MidtransService mock = Mockito.mock(MidtransService.class);

        // Default behavior for QRIS charge - returns successful response
        when(mock.createQrisCharge(anyString(), anyLong())).thenAnswer(invocation -> {
            String orderId = invocation.getArgument(0);
            Map<String, Object> response = new HashMap<>();
            response.put("status_code", "201");
            response.put("status_message", "OK, QRIS created");
            response.put("transaction_id", "TXNID-" + orderId);
            response.put("order_id", orderId);
            response.put("transaction_status", "pending");

            Map<String, Object> actions = new HashMap<>();
            actions.put("url", "https://api.sandbox.midtrans.com/qr/" + orderId);
            response.put("actions", java.util.List.of(actions));

            return response;
        });

        // Default behavior for transaction status
        when(mock.getTransactionStatus(anyString())).thenAnswer(invocation -> {
            String orderId = invocation.getArgument(0);
            Map<String, Object> response = new HashMap<>();
            response.put("status_code", "200");
            response.put("order_id", orderId);
            response.put("transaction_status", "pending");
            return response;
        });

        // Default behavior for cancel
        when(mock.cancelTransaction(anyString())).thenReturn(true);

        // Default behavior for signature verification
        when(mock.verifySignature(anyString(), anyString(), anyString(), anyString())).thenReturn(true);

        // Default behavior for status mapping
        when(mock.mapTransactionStatus(anyString(), any())).thenCallRealMethod();

        // Default behavior for expiry calculation
        when(mock.calculateExpiry()).thenReturn(Instant.now().plusSeconds(900));

        return mock;
    }

    @Bean
    @Primary
    public WhatsAppService whatsAppService() {
        WhatsAppService mock = Mockito.mock(WhatsAppService.class);

        // Default behavior - just log without actually sending
        doNothing().when(mock).sendMessage(anyString(), anyString());

        // Session is always ready in tests
        when(mock.isSessionReady()).thenReturn(true);

        return mock;
    }

    @Bean
    @Primary
    public StorageService storageService() {
        StorageService mock = Mockito.mock(StorageService.class);

        // Default behavior for upload - returns mock URL
        when(mock.upload(any(), anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(1);
            return "https://test.r2.dev/" + key;
        });

        // Default behavior for delete - do nothing
        doNothing().when(mock).delete(anyString());

        // Default behavior for URL building
        when(mock.buildPublicUrl(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return "https://test.r2.dev/" + key;
        });

        when(mock.getPublicUrlBase()).thenReturn("https://test.r2.dev");

        when(mock.extractKey(anyString())).thenAnswer(invocation -> {
            String url = invocation.getArgument(0);
            if (url != null && url.startsWith("https://test.r2.dev/")) {
                return url.substring("https://test.r2.dev/".length());
            }
            return null;
        });

        return mock;
    }

    @Bean
    @Primary
    public com.riakgu.digilo.common.service.EmailService emailService() {
        com.riakgu.digilo.common.service.EmailService mock = 
                Mockito.mock(com.riakgu.digilo.common.service.EmailService.class);

        // Default behavior - just log without actually sending emails
        doNothing().when(mock).sendEmail(anyString(), anyString(), anyString());
        doNothing().when(mock).sendTextEmail(anyString(), anyString(), anyString());

        return mock;
    }
}
