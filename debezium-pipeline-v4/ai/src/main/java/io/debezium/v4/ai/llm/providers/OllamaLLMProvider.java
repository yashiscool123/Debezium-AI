package io.debezium.v4.ai.llm.providers;

import dev.langchain4j.model.ollama.OllamaChatModel;
import io.debezium.v4.ai.llm.LLMService.LLMProvider;
import java.time.Duration;

public class OllamaLLMProvider implements LLMProvider {
    private final OllamaChatModel model;
    private final String modelName;

    public OllamaLLMProvider(String baseUrl, String modelName, int timeoutMs) {
        this.modelName = modelName;
        this.model = OllamaChatModel.builder()
            .baseUrl(baseUrl)
            .modelName(modelName)
            .timeout(Duration.ofMillis(timeoutMs))
            .temperature(0.1)
            .build();
    }

    @Override
    public String generate(String prompt) {
        return model.generate(prompt);
    }

    @Override
    public String name() { return "ollama:" + modelName; }
}
