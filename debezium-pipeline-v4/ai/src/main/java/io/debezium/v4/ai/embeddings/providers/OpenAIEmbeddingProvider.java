package io.debezium.v4.ai.embeddings.providers;

import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModelName;
import io.debezium.v4.ai.embeddings.EmbeddingService.EmbeddingProvider;
import java.time.Duration;

public class OpenAIEmbeddingProvider implements EmbeddingProvider {
    private final OpenAiEmbeddingModel model;
    private final int dimensions;

    public OpenAIEmbeddingProvider(String apiKey, String modelName, String baseUrl, int timeoutMs) {
        var builder = OpenAiEmbeddingModel.builder()
            .apiKey(apiKey != null ? apiKey : System.getenv("OPENAI_API_KEY"))
            .timeout(Duration.ofMillis(timeoutMs));
        if (baseUrl != null) builder.baseUrl(baseUrl);
        try {
            builder.modelName(OpenAiEmbeddingModelName.valueOf(modelName.toUpperCase().replace("-", "_")));
        } catch (IllegalArgumentException e) {
            builder.modelName(OpenAiEmbeddingModelName.TEXT_EMBEDDING_ADA_002);
        }
        this.model = builder.build();
        this.dimensions = modelName != null && modelName.contains("3-large") ? 3072 :
            modelName != null && modelName.contains("3-small") ? 1536 : 1536;
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
