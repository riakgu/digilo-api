package com.riakgu.digilo.common.service;

import com.riakgu.digilo.config.WahaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Generic WhatsApp messaging service using WAHA API.
 * Provides only core functionality - compose messages in your own service.
 */
@Slf4j
@Service("commonWhatsAppService")
@RequiredArgsConstructor
public class WhatsAppService {

    private final WahaProperties wahaProperties;
    private final RestClient.Builder restClientBuilder;

    /**
     * Send a text message via WhatsApp.
     * 
     * @param phone Phone number in format 628xxx, 08xxx, or +628xxx
     * @param message Message content (supports WhatsApp formatting: *bold*, _italic_, ~strikethrough~)
     */
    public void sendMessage(String phone, String message) {
        String chatId = formatChatId(phone);

        Map<String, Object> body = Map.of(
                "chatId", chatId,
                "text", message,
                "session", wahaProperties.session()
        );

        try {
            RestClient client = restClientBuilder.build();
            client.post()
                    .uri(wahaProperties.baseUrl() + "/api/sendText")
                    .header("X-Api-Key", wahaProperties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("WhatsApp message sent to {}", phone);
        } catch (Exception e) {
            log.error("Failed to send WhatsApp message to {}: {}", phone, e.getMessage());
            throw new RuntimeException("Failed to send WhatsApp message", e);
        }
    }

    /**
     * Check if WAHA session is ready.
     */
    public boolean isSessionReady() {
        try {
            RestClient client = restClientBuilder.build();
            ResponseEntity<Map> response = client.get()
                    .uri(wahaProperties.baseUrl() + "/api/sessions/" + wahaProperties.session() + "/me")
                    .header("X-Api-Key", wahaProperties.apiKey())
                    .retrieve()
                    .toEntity(Map.class);

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("WAHA session check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Format phone number to WhatsApp chat ID format.
     * Input: 628123456789 or 08123456789 or +628123456789
     * Output: 628123456789@c.us
     */
    private String formatChatId(String phone) {
        String cleaned = phone.replaceAll("[^0-9]", "");

        if (cleaned.startsWith("0")) {
            cleaned = "62" + cleaned.substring(1);
        }

        return cleaned + "@c.us";
    }
}
