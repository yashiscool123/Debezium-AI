package io.debezium.v4.ai.config;

import io.debezium.v4.ai.llm.providers.*;
import io.debezium.v4.ai.llm.LLMService.LLMProvider;
import java.util.Map;

public record LLMConfig(
    String provider,
    String modelName,
    String apiKey,
    String baseUrl,
    int timeoutMs
) {
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String provider = "ollama";
        private String modelName = "llama3.1";
        private String apiKey;
        private String baseUrl;
        private int timeoutMs = 60000;

        public Builder provider(String v) { this.provider = v; return this; }
        public Builder modelName(String v) { this.modelName = v; return this; }
        public Builder apiKey(String v) { this.apiKey = v; return this; }
        public Builder baseUrl(String v) { this.baseUrl = v; return this; }
        public Builder timeoutMs(int v) { this.timeoutMs = v; return this; }
        public LLMConfig build() { return new LLMConfig(provider, modelName, apiKey, baseUrl, timeoutMs); }
    }

    public static LLMConfig fromProperties(Map<String, String> props) {
        return builder()
            .provider(props.getOrDefault("debezium.ai.llm.provider", "ollama"))
            .modelName(props.getOrDefault("debezium.ai.llm.model", "llama3.1"))
            .apiKey(props.get("debezium.ai.llm.api-key"))
            .baseUrl(props.get("debezium.ai.llm.base-url"))
            .timeoutMs(Integer.parseInt(props.getOrDefault("debezium.ai.llm.timeout-ms", "60000")))
            .build();
    }

    public LLMProvider createProvider() {
        return switch (provider.toLowerCase()) {
            case "openai" -> new OpenAILLMProvider(apiKey, modelName != null ? modelName : "gpt-4o",
                baseUrl, timeoutMs);
            case "anthropic" -> new AnthropicLLMProvider(apiKey, modelName != null ? modelName : "claude-3-opus-20240229",
                baseUrl, timeoutMs);
            case "google" -> new GoogleLLMProvider(apiKey, modelName != null ? modelName : "gemini-1.5-pro",
                baseUrl, timeoutMs);
            default -> new OllamaLLMProvider(baseUrl != null ? baseUrl : "http://localhost:11434",
                modelName != null ? modelName : "llama3.1", timeoutMs);
        };
    }
}
