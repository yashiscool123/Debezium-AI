package io.debezium.v4.api.factory;

import io.debezium.v4.ai.config.ProviderRegistry;
import io.debezium.v4.ai.embeddings.EmbeddingService;
import io.debezium.v4.ai.llm.LLMService;
import io.debezium.v4.ai.quality.AIQualityService;
import io.debezium.v4.ai.vector.VectorStore;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Map;

@ApplicationScoped
public class AIQualityServiceFactory {

    private AIQualityService aiQualityService;

    @ConfigProperty(name = "debezium.ai.embeddings.provider", defaultValue = "minilm")
    String embeddingProvider;

    @ConfigProperty(name = "debezium.ai.llm.provider", defaultValue = "ollama")
    String llmProvider;

    @ConfigProperty(name = "debezium.ai.quality.vector-store-size", defaultValue = "10000")
    int vectorStoreSize;

    @PostConstruct
    void init() {
        EmbeddingService embeddingService = ProviderRegistry.createEmbeddingService(embeddingProvider);
        LLMService llmService = ProviderRegistry.createLLMService(llmProvider);
        VectorStore vectorStore = new VectorStore();
        this.aiQualityService = new AIQualityService(llmService, embeddingService, vectorStore);
    }

    public AIQualityService get() {
        return aiQualityService;
    }

    public AIQualityService createWithConfig(Map<String, String> props) {
        EmbeddingService embeddingService = ProviderRegistry.createEmbeddingService(props);
        LLMService llmService = ProviderRegistry.createLLMService(props);
        return new AIQualityService(llmService, embeddingService);
    }
}
