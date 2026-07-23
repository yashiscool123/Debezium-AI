package io.debezium.v4.ai.embeddings.providers;

import dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel;
import io.debezium.v4.ai.embeddings.EmbeddingService.EmbeddingProvider;
import java.time.Duration;

public class HuggingFaceEmbeddingProvider implements EmbeddingProvider {
    private final HuggingFaceEmbeddingModel model;
    private final int dimensions;

    public HuggingFaceEmbeddingProvider(String apiKey, String modelName, String baseUrl, int timeoutMs) {
        var builder = HuggingFaceEmbeddingModel.builder()
            .accessToken(apiKey != null ? apiKey : System.getenv("HF_API_TOKEN"))
            .modelId(modelName)
            .timeout(Duration.ofMillis(timeoutMs));
        if (baseUrl != null) builder.baseUrl(baseUrl);
        this.model = builder.build();
        this.dimensions = "bert-base-uncased".equalsIgnoreCase(modelName) ? 768 :
            modelName != null && modelName.contains("large") ? 1024 : 768;
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
