package io.debezium.v4.ai.embeddings.providers;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import io.debezium.v4.ai.embeddings.EmbeddingService.EmbeddingProvider;

public class MiniLMEmbeddingProvider implements EmbeddingProvider {
    private final EmbeddingModel model;

    public MiniLMEmbeddingProvider() {
        this.model = new AllMiniLmL6V2EmbeddingModel();
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
    public int dimensions() { return 384; }
}
