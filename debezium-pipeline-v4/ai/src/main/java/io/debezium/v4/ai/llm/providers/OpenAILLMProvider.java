package io.debezium.v4.ai.llm.providers;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import io.debezium.v4.ai.llm.LLMService.LLMProvider;
import java.time.Duration;

public class OpenAILLMProvider implements LLMProvider {
    private final OpenAiChatModel model;
    private final String modelName;

    public OpenAILLMProvider(String apiKey, String modelName, String baseUrl, int timeoutMs) {
        this.modelName = modelName;
        var builder = OpenAiChatModel.builder()
            .apiKey(apiKey != null ? apiKey : System.getenv("OPENAI_API_KEY"))
            .timeout(Duration.ofMillis(timeoutMs))
            .temperature(0.1);
        if (baseUrl != null) builder.baseUrl(baseUrl);
        try {
            builder.modelName(OpenAiChatModelName.valueOf(modelName.toUpperCase().replace("-", "_")));
        } catch (IllegalArgumentException e) {
            builder.modelName(OpenAiChatModelName.GPT_4O);
        }
        this.model = builder.build();
    }

    @Override
    public String generate(String prompt) {
        return model.generate(prompt);
    }

    @Override
    public String name() { return "openai:" + modelName; }
}
