package io.debezium.v4.core.security;

import io.debezium.v4.core.util.CryptoUtil;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.SecretKey;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class SecretManager {

    private static final Set<String> SENSITIVE_KEY_PATTERNS = Set.of(
        "password", "passwd", "pwd", "secret", "token", "api.key", "apikey",
        "api_key", "private.key", "private_key", "ssl.key", "ssl_key",
        "truststore", "keystore", "credentials", "credential", "auth",
        "connection.url", "jdbc.url", "mongodb.uri", "kafka.bootstrap"
    );

    @ConfigProperty(name = "debezium.encryption.master-key", defaultValue = "changeit-changeit-changeit!")
    String masterKey;

    @ConfigProperty(name = "debezium.encryption.salt", defaultValue = "")
    String configuredSalt;

    private SecretKey secretKey;
    private byte[] salt;

    @PostConstruct
    void init() {
        if (!configuredSalt.isEmpty()) {
            this.salt = java.util.Base64.getDecoder().decode(configuredSalt);
        } else {
            this.salt = CryptoUtil.generateSalt();
        }
        this.secretKey = CryptoUtil.deriveKey(masterKey, salt);
    }

    public SecretKey getSecretKey() {
        return secretKey;
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) return plaintext;
        return CryptoUtil.encrypt(plaintext, secretKey);
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) return ciphertext;
        if (!CryptoUtil.isEncrypted(ciphertext)) return ciphertext;
        return CryptoUtil.decrypt(ciphertext, secretKey);
    }

    public String mask(String value) {
        if (value == null || value.length() < 4) return "****";
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }

    public boolean isSensitiveKey(String key) {
        if (key == null) return false;
        String lower = key.toLowerCase().replace("-", ".").replace("_", ".");
        for (String pattern : SENSITIVE_KEY_PATTERNS) {
            if (lower.contains(pattern)) return true;
        }
        return false;
    }

    public Map<String, String> encryptConfig(Map<String, String> config) {
        if (config == null) return Map.of();
        Map<String, String> result = new HashMap<>();
        for (var entry : config.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (isSensitiveKey(key) && value != null && !value.startsWith("ENC(")) {
                result.put(key, "ENC(" + CryptoUtil.encrypt(value, secretKey) + ")");
            } else {
                result.put(key, value);
            }
        }
        return Map.copyOf(result);
    }

    public Map<String, String> decryptConfig(Map<String, String> config) {
        if (config == null) return Map.of();
        Map<String, String> result = new HashMap<>();
        for (var entry : config.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value != null && value.startsWith("ENC(") && value.endsWith(")")) {
                String encrypted = value.substring(4, value.length() - 1);
                try {
                    result.put(key, CryptoUtil.decrypt(encrypted, secretKey));
                } catch (Exception e) {
                    result.put(key, value);
                }
            } else {
                result.put(key, value);
            }
        }
        return Map.copyOf(result);
    }

    public Map<String, String> maskConfig(Map<String, String> config) {
        if (config == null) return Map.of();
        Map<String, String> result = new HashMap<>();
        for (var entry : config.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (isSensitiveKey(key) && value != null && value.length() >= 4) {
                result.put(key, mask(value));
            } else {
                result.put(key, value);
            }
        }
        return Map.copyOf(result);
    }
}
