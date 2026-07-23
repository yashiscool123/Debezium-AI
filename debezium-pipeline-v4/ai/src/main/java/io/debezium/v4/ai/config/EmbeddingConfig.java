package io.debezium.v4.ai.config;

import io.debezium.v4.ai.embeddings.providers.*;
import io.debezium.v4.ai.embeddings.EmbeddingService.EmbeddingProvider;
import java.util.Map;

public record EmbeddingConfig(
    String provider,
    String modelName,
    String apiKey,
    String baseUrl,
    int dimensions,
    int timeoutMs
) {
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String provider = "minilm";
        private String modelName = "all-MiniLM-L6-v2";
        private String apiKey;
        private String baseUrl;
        private int dimensions = 384;
        private int timeoutMs = 15000;

        public Builder provider(String v) { this.provider = v; return this; }
        public Builder modelName(String v) { this.modelName = v; return this; }
        public Builder apiKey(String v) { this.apiKey = v; return this; }
        public Builder baseUrl(String v) { this.baseUrl = v; return this; }
        public Builder dimensions(int v) { this.dimensions = v; return this; }
        public Builder timeoutMs(int v) { this.timeoutMs = v; return this; }
        public EmbeddingConfig build() { return new EmbeddingConfig(provider, modelName, apiKey, baseUrl, dimensions, timeoutMs); }
    }

    public static EmbeddingConfig fromProperties(Map<String, String> props) {
        return builder()
            .provider(props.getOrDefault("debezium.ai.embeddings.provider", "minilm"))
            .modelName(props.getOrDefault("debezium.ai.embeddings.model", "all-MiniLM-L6-v2"))
            .apiKey(props.get("debezium.ai.embeddings.api-key"))
            .baseUrl(props.get("debezium.ai.embeddings.base-url"))
            .dimensions(Integer.parseInt(props.getOrDefault("debezium.ai.embeddings.dimensions", "384")))
            .timeoutMs(Integer.parseInt(props.getOrDefault("debezium.ai.embeddings.timeout-ms", "15000")))
            .build();
    }

    public EmbeddingProvider createProvider() {
        return switch (provider.toLowerCase()) {
            case "ollama" -> new OllamaEmbeddingProvider(baseUrl != null ? baseUrl : "http://localhost:11434",
                modelName != null ? modelName : "nomic-embed-text", timeoutMs);
            case "openai" -> new OpenAIEmbeddingProvider(apiKey, modelName != null ? modelName : "text-embedding-ada-002",
                baseUrl, timeoutMs);
            case "voyageai" -> new VoyageAIEmbeddingProvider(apiKey, modelName != null ? modelName : "voyage-code-2",
                baseUrl, timeoutMs);
            case "huggingface" -> new HuggingFaceEmbeddingProvider(apiKey,
                modelName != null ? modelName : "bert-base-uncased", baseUrl, timeoutMs);
            default -> new MiniLMEmbeddingProvider();
        };
    }
}
