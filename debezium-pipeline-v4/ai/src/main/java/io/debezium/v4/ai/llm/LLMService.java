package io.debezium.v4.ai.llm;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class LLMService {
    private final LLMProvider provider;

    public LLMService(LLMProvider provider) { this.provider = provider; }

    public String ask(String prompt) { return provider.generate(prompt); }
    public CompletableFuture<String> askAsync(String prompt) { return CompletableFuture.supplyAsync(() -> ask(prompt)); }

    public List<Map<String,String>> suggestMappings(String schemaContext) {
        String prompt = """
You are a Debezium CDC mapping expert. Given the source and target schemas below, suggest table and column mappings.
Return the result as a JSON array of objects with: sourceTable, targetTable, sourceColumn, targetColumn, transformation, confidence.

Schema:
"""
        + schemaContext;
        String response = ask(prompt);
        return parseMappingResponse(response);
    }

    public String generateTransformation(String sourceType, String targetType, String expression) {
        String prompt = "Generate a Kafka Connect SMT configuration to transform from " + sourceType
            + " to " + targetType + ". Expression: " + expression
            + "\nReturn only the SMT configuration as JSON.";
        return ask(prompt);
    }

    public String generateKSQL(String sourceTopic, String targetTopic, String mappingDescription) {
        String prompt = "Generate a KSQL query to stream from " + sourceTopic + " to " + targetTopic
            + ". Mapping: " + mappingDescription + "\nReturn only the KSQL query.";
        return ask(prompt);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String,String>> parseMappingResponse(String response) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(response, List.class);
        } catch (Exception e) {
            return List.of();
        }
    }

    public interface LLMProvider {
        String generate(String prompt);
        default String name() { return "default"; }
    }
}
