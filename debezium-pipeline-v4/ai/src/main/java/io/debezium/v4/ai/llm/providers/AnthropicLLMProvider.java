package io.debezium.v4.ai.llm.providers;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import io.debezium.v4.ai.llm.LLMService.LLMProvider;
import java.time.Duration;

public class AnthropicLLMProvider implements LLMProvider {
    private final AnthropicChatModel model;
    private final String modelName;

    public AnthropicLLMProvider(String apiKey, String modelName, String baseUrl, int timeoutMs) {
        this.modelName = modelName;
        var builder = AnthropicChatModel.builder()
            .apiKey(apiKey != null ? apiKey : System.getenv("ANTHROPIC_API_KEY"))
            .modelName(modelName)
            .timeout(Duration.ofMillis(timeoutMs))
            .temperature(0.1)
            .maxTokens(4096);
        if (baseUrl != null) builder.baseUrl(baseUrl);
        this.model = builder.build();
    }

    @Override
    public String generate(String prompt) {
        return model.generate(prompt);
    }

    @Override
    public String name() { return "anthropic:" + modelName; }
}
