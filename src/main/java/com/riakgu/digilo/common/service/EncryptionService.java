package com.riakgu.digilo.common.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.type.TypeReference;
import com.riakgu.digilo.config.EncryptionProperties;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class EncryptionService {

    private final TextEncryptor encryptor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EncryptionService(EncryptionProperties properties) {
        this.encryptor = Encryptors.text(properties.password(), properties.salt());
    }

    public String encrypt(Map<String, Object> data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            return encryptor.encrypt(json);
        } catch (JacksonException e) {
            throw new RuntimeException("Failed to serialize credential", e);
        }
    }

    public Map<String, Object> decrypt(String encryptedData) {
        try {
            String json = encryptor.decrypt(encryptedData);
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JacksonException e) {
            throw new RuntimeException("Failed to deserialize credential", e);
        }
    }
}