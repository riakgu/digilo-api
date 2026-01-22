package com.riakgu.digilo.product;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riakgu.digilo.config.EncryptionProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class EncryptionService {

    private final TextEncryptor encryptor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EncryptionService(EncryptionProperties properties) {
        this.encryptor = Encryptors.text(properties.getPassword(), properties.getSalt());
    }

    public String encrypt(Map<String, Object> data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            return encryptor.encrypt(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize credential", e);
        }
    }

    public Map<String, Object> decrypt(String encryptedData) {
        try {
            String json = encryptor.decrypt(encryptedData);
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize credential", e);
        }
    }
}