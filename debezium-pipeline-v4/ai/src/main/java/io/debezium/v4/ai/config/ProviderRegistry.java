package io.debezium.v4.ai.config;

import io.debezium.v4.ai.embeddings.EmbeddingService;
import io.debezium.v4.ai.embeddings.EmbeddingService.EmbeddingProvider;
import io.debezium.v4.ai.llm.LLMService;
import io.debezium.v4.ai.llm.LLMService.LLMProvider;
import java.util.Map;

public class ProviderRegistry {

    public static EmbeddingService createEmbeddingService(Map<String, String> props) {
        var config = EmbeddingConfig.fromProperties(props);
        return new EmbeddingService(config.createProvider());
    }

    public static LLMService createLLMService(Map<String, String> props) {
        var config = LLMConfig.fromProperties(props);
        return new LLMService(config.createProvider());
    }

    public static EmbeddingService createEmbeddingService(String providerType) {
        var config = EmbeddingConfig.builder().provider(providerType).build();
        return new EmbeddingService(config.createProvider());
    }

    public static LLMService createLLMService(String providerType) {
        var config = LLMConfig.builder().provider(providerType).build();
        return new LLMService(config.createProvider());
    }

    public static Map<String, String> defaultProps() {
        return Map.of(
            "debezium.ai.embeddings.provider", "minilm",
            "debezium.ai.llm.provider", "ollama",
            "debezium.ai.llm.model", "llama3.1",
            "debezium.ai.embeddings.model", "all-MiniLM-L6-v2"
        );
    }
}
