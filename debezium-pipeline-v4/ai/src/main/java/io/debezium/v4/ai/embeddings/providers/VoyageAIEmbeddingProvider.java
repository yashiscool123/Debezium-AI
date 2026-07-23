package io.debezium.v4.ai.embeddings.providers;

import dev.langchain4j.model.voyageai.VoyageAiEmbeddingModel;
import io.debezium.v4.ai.embeddings.EmbeddingService.EmbeddingProvider;
import java.time.Duration;

public class VoyageAIEmbeddingProvider implements EmbeddingProvider {
    private final VoyageAiEmbeddingModel model;
    private final int dimensions;

    public VoyageAIEmbeddingProvider(String apiKey, String modelName, String baseUrl, int timeoutMs) {
        var builder = VoyageAiEmbeddingModel.builder()
            .apiKey(apiKey != null ? apiKey : System.getenv("VOYAGEAI_API_KEY"))
            .modelName(modelName)
            .timeout(Duration.ofMillis(timeoutMs));
        if (baseUrl != null) builder.baseUrl(baseUrl);
        this.model = builder.build();
        this.dimensions = "voyage-code-2".equalsIgnoreCase(modelName) ? 1024 : 1024;
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
