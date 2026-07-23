package io.debezium.v4.ai.embeddings.providers;

import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import io.debezium.v4.ai.embeddings.EmbeddingService.EmbeddingProvider;
import java.time.Duration;

public class OllamaEmbeddingProvider implements EmbeddingProvider {
    private final OllamaEmbeddingModel model;
    private final int dimensions;

    public OllamaEmbeddingProvider(String baseUrl, String modelName, int timeoutMs) {
        this.model = OllamaEmbeddingModel.builder()
            .baseUrl(baseUrl)
            .modelName(modelName)
            .timeout(Duration.ofMillis(timeoutMs))
            .build();
        this.dimensions = "nomic-embed-text".equalsIgnoreCase(modelName) ? 768 : 384;
    }

    @Override
    public float[] embed(String text) {
        var response = model.embed(text);
        var vec = response.content().vector();
        float[] result = new float[vec.length];
        for (int i = 0; i < vec.length; i++) result[i] = vec[i].floatValue();
        return result;
    }

    @Override
    public int dimensions() { return dimensions; }
}
