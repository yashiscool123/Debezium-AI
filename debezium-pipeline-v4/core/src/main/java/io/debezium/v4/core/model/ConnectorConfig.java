package io.debezium.v4.core.model;

import io.debezium.v4.core.security.SecretManager;
import io.debezium.v4.core.util.CryptoUtil;

import java.util.HashMap;
import java.util.Map;

public record ConnectorConfig(
        String connectorClass,
        String name,
        Map<String, String> config,
        Map<String, String> secrets) {

    public static Builder builder() {
        return new Builder();
    }

    public ConnectorConfig withEncryptedSecrets(SecretManager secretManager) {
        if (secrets == null || secrets.isEmpty()) return this;
        Map<String, String> encrypted = new HashMap<>();
        for (var entry : secrets.entrySet()) {
            String value = entry.getValue();
            if (value != null && !CryptoUtil.isEncrypted(value)) {
                encrypted.put(entry.getKey(), secretManager.encrypt(value));
            } else {
                encrypted.put(entry.getKey(), value);
            }
        }
        return new ConnectorConfig(connectorClass, name, config, Map.copyOf(encrypted));
    }

    public ConnectorConfig withDecryptedSecrets(SecretManager secretManager) {
        if (secrets == null || secrets.isEmpty()) return this;
        Map<String, String> decrypted = new HashMap<>();
        for (var entry : secrets.entrySet()) {
            String value = entry.getValue();
            if (value != null && CryptoUtil.isEncrypted(value)) {
                decrypted.put(entry.getKey(), secretManager.decrypt(value));
            } else {
                decrypted.put(entry.getKey(), value);
            }
        }
        return new ConnectorConfig(connectorClass, name, config, Map.copyOf(decrypted));
    }

    public ConnectorConfig withMaskedSecrets(SecretManager secretManager) {
        if (secrets == null || secrets.isEmpty()) return this;
        Map<String, String> masked = new HashMap<>();
        for (var entry : secrets.entrySet()) {
            if (secretManager.isSensitiveKey(entry.getKey())) {
                masked.put(entry.getKey(), secretManager.mask(entry.getValue()));
            } else {
                masked.put(entry.getKey(), entry.getValue());
            }
        }
        return new ConnectorConfig(connectorClass, name, config, Map.copyOf(masked));
    }

    public ConnectorConfig withEncryptedConfig(SecretManager secretManager) {
        Map<String, String> encryptedConfig = secretManager.encryptConfig(config);
        return new ConnectorConfig(connectorClass, name, encryptedConfig, secrets);
    }

    public ConnectorConfig withDecryptedConfig(SecretManager secretManager) {
        Map<String, String> decryptedConfig = secretManager.decryptConfig(config);
        return new ConnectorConfig(connectorClass, name, decryptedConfig, secrets);
    }

    public ConnectorConfig withMaskedConfig(SecretManager secretManager) {
        Map<String, String> maskedConfig = secretManager.maskConfig(config);
        return new ConnectorConfig(connectorClass, name, maskedConfig, secrets);
    }

    public static class Builder {
        private String connectorClass;
        private String name;
        private Map<String, String> config;
        private Map<String, String> secrets;

        public Builder connectorClass(String connectorClass) {
            this.connectorClass = connectorClass;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder config(Map<String, String> config) {
            this.config = config;
            return this;
        }

        public Builder secrets(Map<String, String> secrets) {
            this.secrets = secrets;
            return this;
        }

        public ConnectorConfig build() {
            return new ConnectorConfig(connectorClass, name, config, secrets);
        }
    }
}
