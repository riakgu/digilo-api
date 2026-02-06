package com.riakgu.digilo.util;

import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility to generate encrypted credentials for SQL seed data.
 *
 * USAGE:
 *   1. Update ENCRYPTION_PASSWORD and ENCRYPTION_SALT below to match your .env file:
 *      - ENCRYPTION_PASSWORD = APP_ENCRYPTION_PASSWORD from .env
 *      - ENCRYPTION_SALT = APP_ENCRYPTION_SALT from .env (must be 16 hex characters)
 *   
 *   2. Run this class:
 *      mvn exec:java -D exec.mainClass="com.riakgu.digilo.util.CredentialGenerator" -D exec.classpathScope="test"
 *      Or run directly from IDE (right-click -> Run)
 *   
 *   3. Copy the generated SQL INSERT statements to replace section 7 in:
 *      src/main/resources/db/seed-data.sql
 */
public class CredentialGenerator {

    // ========================================================
    // UPDATE THESE VALUES TO MATCH YOUR .env FILE
    // ========================================================
    private static final String ENCRYPTION_PASSWORD = "your-strong-password-here";  // APP_ENCRYPTION_PASSWORD
    private static final String ENCRYPTION_SALT = "1234567890abcdef";               // APP_ENCRYPTION_SALT (16 hex chars)

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static TextEncryptor encryptor;

    public static void main(String[] args) {
        try {
            encryptor = Encryptors.text(ENCRYPTION_PASSWORD, ENCRYPTION_SALT);
        } catch (Exception e) {
            System.err.println("ERROR: Invalid encryption config. Make sure ENCRYPTION_SALT is a valid hex string.");
            System.err.println("Example hex salt: 5c0744940b5c369b");
            e.printStackTrace();
            return;
        }

        System.out.println("-- =====================================================");
        System.out.println("-- ENCRYPTED CREDENTIALS FOR SEED DATA");
        System.out.println("-- Generated at: " + java.time.Instant.now());
        System.out.println("-- =====================================================");
        System.out.println();
        System.out.println("-- Netflix credentials (variant 1-4)");
        for (int i = 1; i <= 8; i++) {
            String encrypted = encryptAccount("netflix" + i + "@demo.digilo.com", "NetflixPass" + i + "!");
            System.out.println("-- Credential " + i + ": " + encrypted);
        }

        System.out.println();
        System.out.println("-- Spotify credentials (variant 5-8)");
        for (int i = 1; i <= 5; i++) {
            String encrypted = encryptAccount("spotify" + i + "@demo.digilo.com", "SpotifyPass" + i + "!");
            System.out.println("-- Credential " + (8 + i) + ": " + encrypted);
        }

        System.out.println();
        System.out.println("-- Steam Wallet codes (variant 15-18)");
        for (int i = 1; i <= 10; i++) {
            String encrypted = encryptCode("STEAM-" + String.format("%04d", i) + "-DEMO-XXXX", String.format("%04d", 1000 + i));
            System.out.println("-- Code " + (13 + i) + ": " + encrypted);
        }

        System.out.println();
        System.out.println("-- Mobile Legends codes (variant 19-22)");
        for (int i = 1; i <= 10; i++) {
            String encrypted = encryptGameCode("ML-" + String.format("%08d", 10000000 + i), "SEA");
            System.out.println("-- Code " + (23 + i) + ": " + encrypted);
        }

        System.out.println();
        System.out.println("-- =====================================================");
        System.out.println("-- FULL SQL INSERT STATEMENTS");
        System.out.println("-- =====================================================");
        System.out.println();

        generateFullSqlInserts();
    }

    private static final int TOTAL_ITEMS = 40; // Total inventory items
    private static int currentItem = 0;

    private static void generateFullSqlInserts() {
        currentItem = 0;
        System.out.println("INSERT INTO product_inventories (id, variant_id, credential, status, created_at, updated_at) VALUES");

        int id = 1;

        // Netflix 1M (variant 1) - 5 items
        for (int i = 0; i < 5; i++) {
            String encrypted = encryptAccount("netflix" + id + "@demo.digilo.com", "NetflixPass" + id + "!");
            printInsertLine(id, 1, encrypted);
            id++;
        }

        // Netflix 3M (variant 2) - 3 items
        for (int i = 0; i < 3; i++) {
            String encrypted = encryptAccount("netflix" + id + "@demo.digilo.com", "NetflixPass" + id + "!");
            printInsertLine(id, 2, encrypted);
            id++;
        }

        // Spotify 1M (variant 5) - 5 items
        for (int i = 0; i < 5; i++) {
            String encrypted = encryptAccount("spotify" + (i+1) + "@demo.digilo.com", "SpotifyPass" + (i+1) + "!");
            printInsertLine(id, 5, encrypted);
            id++;
        }

        // Steam Wallet 12K (variant 15) - 10 items
        for (int i = 0; i < 10; i++) {
            String encrypted = encryptCode("STEAM-" + String.format("%04d", i+1) + "-DEMO-XXXX", String.format("%04d", 1001 + i));
            printInsertLine(id, 15, encrypted);
            id++;
        }

        // ML Diamonds 86 (variant 19) - 10 items
        for (int i = 0; i < 10; i++) {
            String encrypted = encryptGameCode("ML-" + String.format("%08d", 10000001 + i), "SEA");
            printInsertLine(id, 19, encrypted);
            id++;
        }

        // Additional variants (7 items: variant 3, 4, 6, 7, 8, 9, 10)
        int[] additionalVariants = {3, 4, 6, 7, 8, 9, 10};
        for (int variant : additionalVariants) {
            String encrypted = encryptAccount("demo" + id + "@demo.digilo.com", "DemoPass" + id + "!");
            printInsertLine(id, variant, encrypted);
            id++;
        }

        System.out.println(";");
        System.out.println();
        System.out.println("SELECT setval('product_inventories_id_seq', 100);");
    }

    private static void printInsertLine(int id, int variantId, String encrypted) {
        currentItem++;
        String suffix = (currentItem < TOTAL_ITEMS) ? "," : "";
        System.out.printf("(%d, %d, '%s', 'AVAILABLE', NOW(), NOW())%s%n",
                id, variantId, escaped(encrypted), suffix);
    }

    private static String escaped(String value) {
        return value.replace("'", "''");
    }

    private static String encryptAccount(String email, String password) {
        Map<String, Object> credential = new HashMap<>();
        credential.put("email", email);
        credential.put("password", password);
        credential.put("generatedAt", System.currentTimeMillis());
        return encrypt(credential);
    }

    private static String encryptCode(String code, String pin) {
        Map<String, Object> credential = new HashMap<>();
        credential.put("code", code);
        credential.put("pin", pin);
        credential.put("generatedAt", System.currentTimeMillis());
        return encrypt(credential);
    }

    private static String encryptGameCode(String code, String server) {
        Map<String, Object> credential = new HashMap<>();
        credential.put("code", code);
        credential.put("server", server);
        credential.put("generatedAt", System.currentTimeMillis());
        return encrypt(credential);
    }

    private static String encrypt(Map<String, Object> data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            return encryptor.encrypt(json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt", e);
        }
    }
}
